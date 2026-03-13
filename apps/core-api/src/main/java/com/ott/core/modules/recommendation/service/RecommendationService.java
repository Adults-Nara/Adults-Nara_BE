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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class RecommendationService {

    private final UserPreferenceService userPreferenceService;
    private final UserVectorService userVectorService;
    private final ElasticsearchOperations elasticsearchOperations;
    private final RecommendationQueryBuilder queryBuilder;
    private final VideoFeedEnricher feedEnricher;
    private final Executor executor;

    public RecommendationService(
            UserPreferenceService userPreferenceService,
            UserVectorService userVectorService,
            ElasticsearchOperations elasticsearchOperations,
            RecommendationQueryBuilder queryBuilder,
            VideoFeedEnricher feedEnricher,
            @Qualifier("watchHistoryTaskExecutor") Executor executor) {
        this.userPreferenceService = userPreferenceService;
        this.userVectorService = userVectorService;
        this.elasticsearchOperations = elasticsearchOperations;
        this.queryBuilder = queryBuilder;
        this.feedEnricher = feedEnricher;
        this.executor = executor;
    }

    private static final double FEED_RATIO_PERSONAL = 0.7;
    private static final double FEED_RATIO_POPULAR = 0.2;
    private static final int USER_PREFERENCE_TAG_LIMIT = 5;

    // =========================================================================
    // kNN 벡터 검색 적용
    // =========================================================================
    public List<VideoFeedResponseDto> getPersonalizedFeed(Long userId, VideoType videoType, int page, int size) {

        List<Double> userVector = userVectorService.getUserVector(userId);

        NativeQuery searchQuery;
        if (userVector == null || userVector.isEmpty()) {
            searchQuery = queryBuilder.buildFallbackQuery(videoType, page, size);
        } else {
            searchQuery = queryBuilder.buildMainPersonalizedKnnQuery(userVector, videoType, page, size);
        }

        List<VideoDocument> rawDocuments = executeSearch(searchQuery);
        return feedEnricher.enrich(rawDocuments, userId);
    }
    // =========================================================================
    // [세로 스와이프 피드] - 7(취향) : 2(인기) : 1(랜덤)
    // =========================================================================
    public List<VideoFeedResponseDto> getVerticalMixedFeed(Long userId, VideoType videoType, int size) {
        // 광고 편성 계산 (40% 확률, 사이즈가 최소 3개 이상일 때만)
        boolean shouldInjectAd = size > 2 && Math.random() < 0.4;
        int organicSize = shouldInjectAd ? size - 1 : size; // 일반 영상 개수 할당
        int personalSize = (int) Math.round(size * FEED_RATIO_PERSONAL);
        int popularSize = (int) Math.round(size * FEED_RATIO_POPULAR);
        int randomSize = size - personalSize - popularSize;

        List<TagScoreDto> userPreferences = userPreferenceService.getTopPreferences(userId, USER_PREFERENCE_TAG_LIMIT);
        // 취향 영상 (개인화)
        CompletableFuture<List<VideoDocument>> personalFuture = CompletableFuture.supplyAsync(() ->
                        executeSearch(userPreferences.isEmpty()
                                ? queryBuilder.buildPopularQuery(videoType,personalSize)
                                : queryBuilder.buildMainPersonalizedQuery(userPreferences, videoType, 0, personalSize))
                , executor);
        // 인기 영상
        CompletableFuture<List<VideoDocument>> popularFuture = CompletableFuture.supplyAsync(() ->
                        executeSearch(queryBuilder.buildPopularQuery(videoType,popularSize + 5))
                , executor);
        // 랜덤 영상
        CompletableFuture<List<VideoDocument>> randomFuture = CompletableFuture.supplyAsync(() ->
                        executeSearch(queryBuilder.buildRandomQuery(videoType,randomSize + 5))
                , executor);

        // 광고 영상
        CompletableFuture<List<VideoDocument>> adFuture = shouldInjectAd
                ? CompletableFuture.supplyAsync(() -> executeSearch(queryBuilder.buildPersonalizedAdQuery(userPreferences, videoType)), executor)
                : CompletableFuture.completedFuture(Collections.emptyList());

        CompletableFuture.allOf(personalFuture, popularFuture, randomFuture, adFuture).join();

        // 비디오 ID를 기준으로 완벽하게 중복을 제거하면서 피드 병합 (Set<Long> 활용)
        Set<Long> seenVideoIds = new HashSet<>();
        List<VideoDocument> mixedFeed = new ArrayList<>();

        // 1. 취향 영상 담기
        for (VideoDocument doc : personalFuture.join()) {
            if (seenVideoIds.add(doc.getVideoId())) {
                mixedFeed.add(doc);
            }
        }

        // 2. 인기 영상 담기 (목표 사이즈를 채울 때까지)
        int targetSizeAfterPopular = personalSize + popularSize;
        for (VideoDocument doc : popularFuture.join()) {
            if (mixedFeed.size() < targetSizeAfterPopular && seenVideoIds.add(doc.getVideoId())) {
                mixedFeed.add(doc);
            }
        }

        // 3. 랜덤 영상 담기 (최종 목표 사이즈를 채울 때까지)
        for (VideoDocument doc : randomFuture.join()) {
            if (mixedFeed.size() < size && seenVideoIds.add(doc.getVideoId())) {
                mixedFeed.add(doc);
            }
        }
        // 4. 자연스러운 위치에 광고 주입
        if (shouldInjectAd && !adFuture.join().isEmpty()) {
            VideoDocument adDoc = adFuture.join().get(0);

            // 첫 번째 영상(인덱스 0)은 일반 영상으로 두고, 2~6번째 사이(인덱스 1~5) 랜덤 삽입
            int maxInsertIndex = Math.min(mixedFeed.size(), 6);
            int insertIndex = maxInsertIndex > 1 ? (int) (Math.random() * (maxInsertIndex - 1)) + 1 : 1;

            if (insertIndex > mixedFeed.size()) {
                insertIndex = mixedFeed.size();
            }

            mixedFeed.add(insertIndex, adDoc); // 리스트 중간으로 광고가 쏙 밀고 들어갑니다!
        }
        return feedEnricher.enrich(mixedFeed, userId);
    }

    // =========================================================================
    //  [가로 스와이프 피드] - 상세페이지 연관 영상 추천
    // =========================================================================
    public List<VideoFeedResponseDto> getHorizontalRelatedVideos(Long videoId, Long currentUserId, VideoType videoType, int page, int size) {
        VideoDocument currentVideo = elasticsearchOperations.get(videoId.toString(), VideoDocument.class);

        if (currentVideo == null || currentVideo.getTags() == null || currentVideo.getTags().isEmpty()) {
            return List.of();
        }

        List<FieldValue> tagValues = currentVideo.getTags().stream().map(FieldValue::of).toList();
        NativeQuery searchQuery = queryBuilder.buildRelatedQuery(tagValues, currentVideo.getVideoId(), videoType, page, size);

        List<VideoDocument> rawDocuments = executeSearch(searchQuery);
        return feedEnricher.enrich(rawDocuments, currentUserId);
    }

    // 엘라스틱서치 실행 공통 헬퍼 메서드
    private List<VideoDocument> executeSearch(NativeQuery query) {
        return elasticsearchOperations.search(query, VideoDocument.class).getSearchHits().stream()
                .map(SearchHit::getContent).toList();
    }
}