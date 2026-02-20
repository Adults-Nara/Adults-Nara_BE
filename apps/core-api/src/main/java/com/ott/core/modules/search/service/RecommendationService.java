package com.ott.core.modules.search.service;

import co.elastic.clients.elasticsearch._types.query_dsl.FieldValueFactorModifier;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionBoostMode;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScore;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScoreQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.ott.core.modules.preference.dto.TagScoreDto;
import com.ott.core.modules.preference.service.UserPreferenceService;
import com.ott.core.modules.search.document.VideoDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final UserPreferenceService userPreferenceService;
    private final ElasticsearchOperations elasticsearchOperations;

    /**
     * 메인 피드 맞춤형 추천 비디오 목록 조회
     */
    public List<VideoDocument> getPersonalizedFeed(Long userId, int page, int size) {
        // 1. Redis(또는 DB)에서 유저의 최애 태그 Top 5를 가져온다. (Cache-Aside 발동!)
        List<TagScoreDto> userPreferences = userPreferenceService.getTopPreferences(userId, 5);


        NativeQuery searchQuery = userPreferences.isEmpty()
                ? buildFallbackQuery(page, size)
                : buildPersonalizedQuery(userPreferences, page, size);

        // 쿼리를 Elasticsearch에 날려서 결과 받기
        SearchHits<VideoDocument> searchHits = elasticsearchOperations.search(searchQuery, VideoDocument.class);

        // 실제 Document 객체만 쏙 뽑아서 리스트로 반환합니다.
        return searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .toList();
    }

    private NativeQuery buildFallbackQuery(int page, int size) {
        // ==========================================
        // [Fallback] 취향 데이터가 없는 신규 유저
        // ==========================================
        log.info("[추천 API] 신규 유저입니다. 인기/최신순 기본 피드를 제공합니다.");
        return NativeQuery.builder()
                .withQuery(q -> q.matchAll(m -> m)) // 조건 없이 전부 다
                .withSort(Sort.by(Sort.Direction.DESC, "viewCount")) // 1순위: 조회수 높은 순
                .withSort(Sort.by(Sort.Direction.DESC, "createdAt")) // 2순위: 최신순
                .withPageable(PageRequest.of(page, size))
                .build();
    }

    private NativeQuery buildPersonalizedQuery(List<TagScoreDto> userPreferences, int page, int size) {
        // ==========================================
        // [Personalized] 취향 데이터가 있는 기존 유저
        // ==========================================
        log.info("[추천 API] 맞춤형 추천 시작! 선호 태그 개수: {}", userPreferences.size());

        List<FunctionScore> functions = new ArrayList<>();

        // 가중치 1: 유저의 취향 태그 매칭 (태그 점수만큼 가중치 부여)
        for (TagScoreDto pref : userPreferences) {

            // 방어 로직: 점수가 0 이하면(싫어하는 태그면) 가중치 계산에서 제외합니다!
            if (pref.score() <= 0) {
                continue;
            }
            functions.add(FunctionScore.of(f -> f
                    .filter(fq -> fq.term(t -> t.field("tags").value(pref.tagName())))
                    .weight(pref.score()) // ex: SF태그면 점수 x 9.0배!
            ));
        }

        // 가중치 2: 대중성 (조회수)
        // 조회수가 높을수록 점수를 완만하게 올려줍니다 (Log1p 사용)
        functions.add(FunctionScore.of(f -> f
                .fieldValueFactor(fv -> fv
                        .field("viewCount")
                        .modifier(FieldValueFactorModifier.Log1p)
                        .factor(0.1) // 조회수 영향력 조절
                )
        ));

        // Function Score 쿼리 조립
        Query functionScoreQuery = FunctionScoreQuery.of(fsq -> fsq
                .query(q -> q.matchAll(m -> m)) // 일단 다 가져와서
                .functions(functions)           // 위에서 만든 채점표로 점수를 매김
                .scoreMode(FunctionScoreMode.Sum) // 채점표 점수들을 다 더함
                .boostMode(FunctionBoostMode.Multiply) // 최종 점수에 곱함
        )._toQuery();

        return NativeQuery.builder()
                .withQuery(functionScoreQuery)
                .withPageable(PageRequest.of(page, size))
                .build();
    }
}

