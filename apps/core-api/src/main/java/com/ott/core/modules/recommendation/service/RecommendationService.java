package com.ott.core.modules.recommendation.service;

import co.elastic.clients.elasticsearch._types.FieldValue;
import com.ott.common.persistence.enums.VideoType;
import com.ott.core.modules.preference.dto.TagScoreDto;
import com.ott.core.modules.preference.service.UserPreferenceService;
import com.ott.core.modules.preference.service.UserVectorService;
import com.ott.core.modules.recommendation.component.RecommendationQueryBuilder;
import com.ott.core.modules.recommendation.component.VideoFeedEnricher;
import com.ott.core.modules.recommendation.dto.VideoFeedResponseDto;
import com.ott.core.modules.search.document.VideoDocument;
import com.ott.core.modules.watch.repository.WatchHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class RecommendationService {

    private final UserPreferenceService userPreferenceService;
    private final UserVectorService userVectorService;
    private final ElasticsearchOperations elasticsearchOperations;
    private final RecommendationQueryBuilder queryBuilder;
    private final VideoFeedEnricher feedEnricher;
    private final WatchHistoryRepository watchHistoryRepository;
    private final Executor executor;

    public RecommendationService(
            UserPreferenceService userPreferenceService,
            UserVectorService userVectorService,
            ElasticsearchOperations elasticsearchOperations,
            RecommendationQueryBuilder queryBuilder,
            VideoFeedEnricher feedEnricher,
            WatchHistoryRepository watchHistoryRepository,
            @Qualifier("searchTaskExecutor") Executor executor) {
        this.userPreferenceService = userPreferenceService;
        this.userVectorService = userVectorService;
        this.elasticsearchOperations = elasticsearchOperations;
        this.queryBuilder = queryBuilder;
        this.feedEnricher = feedEnricher;
        this.watchHistoryRepository = watchHistoryRepository;
        this.executor = executor;
    }

    private static final double FEED_RATIO_PERSONAL = 0.7;
    private static final double FEED_RATIO_POPULAR = 0.2;
    private static final int USER_PREFERENCE_TAG_LIMIT = 5;
    private static final int MIN_FEED_SIZE_FOR_AD = 3;
    private static final double AD_INJECTION_PROBABILITY = 1.0; //개발단계 테스트를 위해 광고 확률 100처 원래는 0.4
    private static final int MAX_AD_INSERT_INDEX = 6;

    // 유저의 최근 시청 이력 200개를 가져와서 String 리스트로 반환
    private List<String> getRecentWatchedVideoIds(Long userId) {
        if (userId == null) return new ArrayList<>();
        // 성능을 위해 딱 최근 200개만 가져옵니다.
        List<Long> watchedIds = watchHistoryRepository.findRecentWatchedVideoIds(userId, PageRequest.of(0, 200));
        return watchedIds.stream().map(String::valueOf).toList();
    }
    // =========================================================================
    // kNN 벡터 검색 적용
    // =========================================================================
    public List<VideoFeedResponseDto> getPersonalizedFeed(Long userId, VideoType videoType, int page, int size) {
        List<Float> userVector = userVectorService.getUserVector(userId);

        List<String> excludedIds = getRecentWatchedVideoIds(userId);

        NativeQuery searchQuery;
        if (userVector == null || userVector.isEmpty()) {
            searchQuery = queryBuilder.buildFallbackQuery(videoType, page, size, excludedIds);
        } else {
            searchQuery = queryBuilder.buildMainPersonalizedKnnQuery(userVector, videoType, page, size, excludedIds);
        }

        List<VideoDocument> rawDocuments = executeSearch(searchQuery);
        return feedEnricher.enrich(rawDocuments, userId);
    }
    // =========================================================================
    // [세로 스와이프 피드] - 7(취향) : 2(인기) : 1(랜덤)
    // =========================================================================
    public List<VideoFeedResponseDto> getVerticalMixedFeed(Long userId, VideoType videoType, int page, int size) {
        // 광고 편성 계산 (40% 확률, 사이즈가 최소 3개 이상일 때만)
        boolean shouldInjectAd = size >= MIN_FEED_SIZE_FOR_AD &&
                ThreadLocalRandom.current().nextDouble() < AD_INJECTION_PROBABILITY;
        int organicSize = shouldInjectAd ? size - 1 : size; // 일반 영상 개수 할당
        int personalSize = (int) Math.round(organicSize * FEED_RATIO_PERSONAL);
        int popularSize = (int) Math.round(organicSize * FEED_RATIO_POPULAR);

        List<TagScoreDto> userPreferences = userPreferenceService.getTopPreferences(userId, USER_PREFERENCE_TAG_LIMIT);
        List<String> excludedIds = getRecentWatchedVideoIds(userId);
        int fallbackFetchSize = organicSize + 5; //Over-fetching
        // 취향 영상 (개인화)
        CompletableFuture<List<VideoDocument>> personalFuture = CompletableFuture.supplyAsync(() ->
                        executeSearch(userPreferences.isEmpty()
                                ? queryBuilder.buildFallbackQuery(videoType, 0, personalSize, excludedIds)
                                : queryBuilder.buildMainPersonalizedQuery(userPreferences, videoType, 0, personalSize, excludedIds))
                , executor);
        // 인기 영상
        CompletableFuture<List<VideoDocument>> popularFuture = CompletableFuture.supplyAsync(() ->
                        executeSearch(queryBuilder.buildPopularQuery(videoType, 0, fallbackFetchSize, excludedIds))
                , executor);
        // 랜덤 영상
        CompletableFuture<List<VideoDocument>> randomFuture = CompletableFuture.supplyAsync(() ->
                        executeSearch(queryBuilder.buildRandomQuery(videoType, 0, fallbackFetchSize, excludedIds))
                , executor);

        // 광고 영상
        CompletableFuture<List<VideoDocument>> adFuture = shouldInjectAd
                ? CompletableFuture.supplyAsync(() -> executeSearch(queryBuilder.buildPersonalizedAdQuery(userPreferences, videoType)), executor)
                : CompletableFuture.completedFuture(Collections.emptyList());

        CompletableFuture.allOf(personalFuture, popularFuture, randomFuture, adFuture).join();

        // 비디오 ID를 기준으로 완벽하게 중복을 제거하면서 피드 병합 (Set<Long> 활용)
        Set<Long> seenVideoIds = new HashSet<>();
        List<VideoDocument> organicFeed = new ArrayList<>();

        // 1. 취향 영상 담기
        for (VideoDocument doc : personalFuture.join()) {
            if (seenVideoIds.add(doc.getVideoId())) organicFeed.add(doc);
        }
        // 백필링 핵심 로직: 취향 영상에서 부족했던 개수(Shortfall)를 구해서 인기 영상의 몫으로 넘겨줌
        int personalShortfall = Math.max(0, personalSize - organicFeed.size());
        int targetSizeAfterPopular = organicFeed.size() + popularSize + personalShortfall;

        for (VideoDocument doc : popularFuture.join()) {
            if (organicFeed.size() < targetSizeAfterPopular && seenVideoIds.add(doc.getVideoId())) {
                organicFeed.add(doc);
            }
        }

        // 3. 랜덤 영상 담기 (최종 목표 사이즈를 채울 때까지)
        for (VideoDocument doc : randomFuture.join()) {
            if (organicFeed.size() < organicSize && seenVideoIds.add(doc.getVideoId())) {
                organicFeed.add(doc);
            }
        }
        // 4. 광고 주입
        if (shouldInjectAd) {
            List<VideoDocument> adDocs = adFuture.join();
            if (!adDocs.isEmpty()) {
                VideoDocument adDoc = adDocs.get(0);
                int maxInsertIndex = Math.min(organicFeed.size(), MAX_AD_INSERT_INDEX);
                int insertIndex = maxInsertIndex > 1 ? ThreadLocalRandom.current().nextInt(1, maxInsertIndex) : 1;

                if (insertIndex > organicFeed.size()) {
                    insertIndex = organicFeed.size();
                }
                organicFeed.add(insertIndex, adDoc);
            }
        }

        return feedEnricher.enrich(organicFeed, userId);
    }

    // =========================================================================
    //  [가로 스와이프 피드] - 상세페이지 연관 영상 추천
    // =========================================================================
    public List<VideoFeedResponseDto> getHorizontalRelatedVideos(Long videoId, Long currentUserId, VideoType videoType, int page, int size) {
        VideoDocument currentVideo = elasticsearchOperations.get(videoId.toString(), VideoDocument.class);

        if (currentVideo == null || currentVideo.getTags() == null || currentVideo.getTags().isEmpty()) {
            return List.of();
        }

        List<String> excludedIds = new ArrayList<>(getRecentWatchedVideoIds(currentUserId));
        excludedIds.add(String.valueOf(videoId));

        List<FieldValue> tagValues = currentVideo.getTags().stream().map(FieldValue::of).toList();
        NativeQuery searchQuery = queryBuilder.buildRelatedQuery(tagValues, videoType, page, size, excludedIds);

        List<VideoDocument> rawDocuments = executeSearch(searchQuery);
        return feedEnricher.enrich(rawDocuments, currentUserId);
    }

    // 엘라스틱서치 실행 공통 헬퍼 메서드
    private List<VideoDocument> executeSearch(NativeQuery query) {
        return elasticsearchOperations.search(query, VideoDocument.class).getSearchHits().stream()
                .map(SearchHit::getContent).toList();
    }
}