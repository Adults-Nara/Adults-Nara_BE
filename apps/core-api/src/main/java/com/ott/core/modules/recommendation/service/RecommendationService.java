package com.ott.core.modules.recommendation.service;

import co.elastic.clients.elasticsearch._types.FieldValue;
import com.ott.common.persistence.enums.VideoType;
import com.ott.core.modules.preference.dto.TagScoreDto;
import com.ott.core.modules.preference.service.UserPreferenceService;
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
    private final ElasticsearchOperations elasticsearchOperations;
    private final RecommendationQueryBuilder queryBuilder;
    private final VideoFeedEnricher feedEnricher;
    private final Executor executor;

    public RecommendationService(
            UserPreferenceService userPreferenceService,
            ElasticsearchOperations elasticsearchOperations,
            RecommendationQueryBuilder queryBuilder,
            VideoFeedEnricher feedEnricher,
            @Qualifier("watchHistoryTaskExecutor") Executor executor) {
        this.userPreferenceService = userPreferenceService;
        this.elasticsearchOperations = elasticsearchOperations;
        this.queryBuilder = queryBuilder;
        this.feedEnricher = feedEnricher;
        this.executor = executor;
    }

    private static final double FEED_RATIO_PERSONAL = 0.7;
    private static final double FEED_RATIO_POPULAR = 0.2;
    private static final int USER_PREFERENCE_TAG_LIMIT = 5;

    // =========================================================================
    // [메인 홈 피드]
    // =========================================================================
    public List<VideoFeedResponseDto> getPersonalizedFeed(Long userId, VideoType videoType, int page, int size) {
        List<TagScoreDto> userPreferences = userPreferenceService.getTopPreferences(userId, USER_PREFERENCE_TAG_LIMIT);

        NativeQuery searchQuery = userPreferences.isEmpty()
                ? queryBuilder.buildFallbackQuery(videoType, page, size)
                : queryBuilder.buildMainPersonalizedQuery(userPreferences, videoType, page, size);

        List<VideoDocument> rawDocuments = executeSearch(searchQuery);
        return feedEnricher.enrich(rawDocuments, userId);
    }
    
    // =========================================================================
    // [세로 스와이프 피드] - 7(취향) : 2(인기) : 1(랜덤)
    // =========================================================================
    public List<VideoFeedResponseDto> getVerticalMixedFeed(Long userId, VideoType videoType, int size) {
        int personalSize = (int) Math.round(size * FEED_RATIO_PERSONAL);
        int popularSize = (int) Math.round(size * FEED_RATIO_POPULAR);
        int randomSize = size - personalSize - popularSize;

        List<TagScoreDto> userPreferences = userPreferenceService.getTopPreferences(userId, USER_PREFERENCE_TAG_LIMIT);
        // 취향 영상 (개인화)
        CompletableFuture<List<VideoDocument>> personalFuture = CompletableFuture.supplyAsync(() ->
                        executeSearch(userPreferences.isEmpty()
                                ? queryBuilder.buildPopularQuery(videoType, personalSize)
                                : queryBuilder.buildMainPersonalizedQuery(userPreferences, videoType,0, personalSize))
                , executor);
        // 인기 영상
        CompletableFuture<List<VideoDocument>> popularFuture = CompletableFuture.supplyAsync(() ->
                        executeSearch(queryBuilder.buildPopularQuery(videoType,popularSize + 5))
                , executor);
        // 랜덤 영상
        CompletableFuture<List<VideoDocument>> randomFuture = CompletableFuture.supplyAsync(() ->
                        executeSearch(queryBuilder.buildRandomQuery(videoType,randomSize + 5))
                , executor);

        CompletableFuture.allOf(personalFuture, popularFuture, randomFuture).join();

        Set<VideoDocument> mixedFeed = new LinkedHashSet<>(personalFuture.join());
        for (VideoDocument doc : popularFuture.join()) {
            if (mixedFeed.size() < personalSize + popularSize) mixedFeed.add(doc);
        }
        for (VideoDocument doc : randomFuture.join()) {
            if (mixedFeed.size() < size) mixedFeed.add(doc);
        }

        return feedEnricher.enrich(new ArrayList<>(mixedFeed), userId);
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