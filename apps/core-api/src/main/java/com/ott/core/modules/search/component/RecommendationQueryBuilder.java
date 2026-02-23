package com.ott.core.modules.search.component;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import com.ott.core.modules.preference.dto.TagScoreDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RecommendationQueryBuilder {

    // ==========================================
    // 1. [메인 피드용] 취향 + 조회수 가중치 쿼리
    // ==========================================
    public NativeQuery buildMainPersonalizedQuery(List<TagScoreDto> userPreferences, int page, int size) {
        List<FunctionScore> functions = new ArrayList<>();

        // 가중치 1: 태그 점수
        for (TagScoreDto pref : userPreferences) {
            if (pref.score() <= 0) continue;
            functions.add(FunctionScore.of(f -> f
                    .filter(fq -> fq.term(t -> t.field("tags").value(pref.tagName())))
                    .weight(pref.score())
            ));
        }

        // 가중치 2: 대중성 (조회수 Log1p 적용)
        functions.add(FunctionScore.of(f -> f
                .fieldValueFactor(fv -> fv
                        .field("viewCount")
                        .modifier(FieldValueFactorModifier.Log1p)
                        .factor(0.1)
                )
        ));

        Query functionScoreQuery = FunctionScoreQuery.of(fsq -> fsq
                .query(q -> q.matchAll(m -> m))
                .functions(functions)
                .scoreMode(FunctionScoreMode.Sum)
                .boostMode(FunctionBoostMode.Multiply)
        )._toQuery();

        return NativeQuery.builder()
                .withQuery(functionScoreQuery)
                .withPageable(PageRequest.of(page, size))
                .build();
    }
    // ==========================================
    // 2. [메인 피드용] 신규 유저 Fallback 쿼리
    // ==========================================
    public NativeQuery buildFallbackQuery(int page, int size) {
        return NativeQuery.builder()
                .withQuery(q -> q.matchAll(m -> m))
                .withSort(Sort.by(Sort.Direction.DESC, "viewCount")) // 1순위: 인기순
                .withSort(Sort.by(Sort.Direction.DESC, "createdAt")) // 2순위: 최신순
                .withPageable(PageRequest.of(page, size))
                .build();
    }

    // [가로 피드] 연관 영상 (More Like This / Terms) - 필터링 제거됨
    public NativeQuery buildRelatedQuery(List<FieldValue> tagValues, Long currentVideoId, int limit) {
        Query relatedQuery = Query.of(q -> q.bool(b -> b
                .must(m -> m.terms(t -> t.field("tags").terms(tf -> tf.value(tagValues))))
                .mustNot(mn -> mn.term(t -> t.field("id").value(currentVideoId))) // 자기 자신만 제외
        ));

        return NativeQuery.builder()
                .withQuery(relatedQuery)
                .withSort(Sort.by(Sort.Direction.DESC, "viewCount"))
                .withPageable(PageRequest.of(0, limit))
                .build();
    }

    // [세로 피드: 인기] 단순 조회수 정렬 쿼리: 2
    public NativeQuery buildPopularQuery(int limit) {
        return NativeQuery.builder()
                .withQuery(q -> q.matchAll(m -> m))
                .withSort(Sort.by(Sort.Direction.DESC, "viewCount"))
                .withPageable(PageRequest.of(0, limit))
                .build();
    }

    // [세로 피드: 랜덤] 엘라스틱서치 random_score 쿼리
    public NativeQuery buildRandomQuery(int limit) {
        Query randomQuery = FunctionScoreQuery.of(fsq -> fsq
                .query(q -> q.matchAll(m -> m))
                .functions(FunctionScore.of(f -> f.randomScore(rs -> rs)))
        )._toQuery();

        return NativeQuery.builder()
                .withQuery(randomQuery)
                .withPageable(PageRequest.of(0, limit))
                .build();
    }
}