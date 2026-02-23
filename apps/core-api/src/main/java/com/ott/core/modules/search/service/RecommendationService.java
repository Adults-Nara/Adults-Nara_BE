package com.ott.core.modules.search.service;

import co.elastic.clients.elasticsearch._types.FieldValue;
import com.ott.core.modules.preference.dto.TagScoreDto;
import com.ott.core.modules.preference.service.UserPreferenceService;
import com.ott.core.modules.search.component.RecommendationQueryBuilder;
import com.ott.core.modules.search.document.VideoDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final UserPreferenceService userPreferenceService;
    private final ElasticsearchOperations elasticsearchOperations;
    private final RecommendationQueryBuilder queryBuilder; // ✅ 쿼리 공장 주입!

    // =========================================================================
    // [메인 홈 피드]
    // =========================================================================
    public List<VideoDocument> getPersonalizedFeed(Long userId, int page, int size) {
        List<TagScoreDto> userPreferences = userPreferenceService.getTopPreferences(userId, 5);

        // 빌더에게 쿼리 조립을 시킵니다!
        NativeQuery searchQuery = userPreferences.isEmpty()
                ? queryBuilder.buildFallbackQuery(page, size)
                : queryBuilder.buildMainPersonalizedQuery(userPreferences, page, size);

        return executeSearch(searchQuery);
    }

    // =========================================================================
    // [세로 스와이프 피드] - 7(취향) : 2(인기) : 1(랜덤) 병렬 믹스
    // =========================================================================
    public List<VideoDocument> getVerticalMixedFeed(Long userId, int size) {
        int personalSize = (int) Math.round(size * 0.7);
        int popularSize = (int) Math.round(size * 0.2);
        int randomSize = size - personalSize - popularSize;

        List<TagScoreDto> userPreferences = userPreferenceService.getTopPreferences(userId, 5);

        // 비동기 병렬 처리
        CompletableFuture<List<VideoDocument>> personalFuture = CompletableFuture.supplyAsync(() ->
                executeSearch(userPreferences.isEmpty()
                        ? queryBuilder.buildPopularQuery(personalSize)
                        : queryBuilder.buildMainPersonalizedQuery(userPreferences, 0, personalSize)) // 기존 쿼리 재활용!
        );

        CompletableFuture<List<VideoDocument>> popularFuture = CompletableFuture.supplyAsync(() ->
                executeSearch(queryBuilder.buildPopularQuery(popularSize + 5))
        );

        CompletableFuture<List<VideoDocument>> randomFuture = CompletableFuture.supplyAsync(() ->
                executeSearch(queryBuilder.buildRandomQuery(randomSize + 5))
        );

        CompletableFuture.allOf(personalFuture, popularFuture, randomFuture).join();

        // 중복 제거 및 조립
        Set<VideoDocument> mixedFeed = new LinkedHashSet<>(personalFuture.join());

        for (VideoDocument doc : popularFuture.join()) {
            if (mixedFeed.size() < personalSize + popularSize) mixedFeed.add(doc);
        }
        for (VideoDocument doc : randomFuture.join()) {
            if (mixedFeed.size() < size) mixedFeed.add(doc);
        }

        return new ArrayList<>(mixedFeed);
    }

    // =========================================================================
    //  [가로 스와이프 피드] - 상세페이지 연관 영상 추천
    // =========================================================================
    public List<VideoDocument> getHorizontalRelatedVideos(Long videoMetadataId, int size) {
        VideoDocument currentVideo = elasticsearchOperations.get(videoMetadataId.toString(), VideoDocument.class);
        if (currentVideo == null || currentVideo.getTags().isEmpty()) {
            return List.of();
        }

        List<FieldValue> tagValues = currentVideo.getTags().stream()
                .map(FieldValue::of)
                .toList();

        NativeQuery searchQuery = queryBuilder.buildRelatedQuery(tagValues, videoMetadataId, size);
        return executeSearch(searchQuery);
    }

    // 엘라스틱서치 실행 공통 헬퍼 메서드
    private List<VideoDocument> executeSearch(NativeQuery query) {
        return elasticsearchOperations.search(query, VideoDocument.class).getSearchHits().stream()
                .map(SearchHit::getContent).toList();
    }
}