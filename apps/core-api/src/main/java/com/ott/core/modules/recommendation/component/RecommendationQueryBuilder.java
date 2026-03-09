package com.ott.core.modules.recommendation.component;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import com.ott.common.persistence.enums.VideoType;
import com.ott.core.modules.preference.dto.TagScoreDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RecommendationQueryBuilder {

    private Query baseActiveVideoQuery(VideoType videoType) {
        return Query.of(q -> q.bool(b -> b
            .filter(f -> f.term(t -> t.field("deleted").value(false)))
            .filter(f -> f.term(t -> t.field("videoType").value(videoType.name())))
        ));
    }

    // ==========================================
    // [메인 피드용] 유저 취향 벡터 기반 kNN 쿼리
    // ==========================================
    public NativeQuery buildMainPersonalizedKnnQuery(List<Double> userVector, VideoType videoType, int page, int size) {
        // 벡터가 없으면 Fallback 쿼리 반환 로직을 Service에서 처리

        int cappedSize = Math.min(size, 100); // 보안 방어 로직: size 상한선을 100으로 제한

        // kNN 쿼리 내부에 deleted=false 필터 결합
        Query filterQuery = baseActiveVideoQuery(videoType);

        // 후보군 넉넉히 잡되, DoS 방지를 위해 최대 500으로 제한
        int numCandidates = Math.min(Math.max(50, cappedSize * 5), 500);

        List<Float> floatVector = userVector.stream().map(Double::floatValue).toList();
        Query knnQuery = Query.of(q -> q
                .knn(k -> k
                        .field("embedding")
                        .queryVector(floatVector)
                        .k(cappedSize)
                        .numCandidates(numCandidates)
                        .filter(filterQuery)
                )
        );

        return NativeQuery.builder()
                .withQuery(knnQuery)
                .withPageable(PageRequest.of(page, size))
                .build();

    }
    // ==========================================
    // 2. [메인 피드용] 신규 유저 Fallback 쿼리
    // ==========================================
    public NativeQuery buildMainPersonalizedQuery(List<TagScoreDto> userPreferences, VideoType videoType, int page, int size) {
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
                .query(baseActiveVideoQuery(videoType))
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
    public NativeQuery buildFallbackQuery(VideoType videoType, int page, int size) {
        return NativeQuery.builder()
                .withQuery(baseActiveVideoQuery(videoType))
                .withSort(Sort.by(Sort.Direction.DESC, "viewCount")) // 1순위: 인기순
                .withSort(Sort.by(Sort.Direction.DESC, "createdAt")) // 2순위: 최신순
                .withPageable(PageRequest.of(page, size))
                .build();
    }

    // 세로 피드 (20%): 인기순 쿼리
    public NativeQuery buildPopularQuery(VideoType videoType, int limit) {
        return NativeQuery.builder()
                .withQuery(baseActiveVideoQuery(videoType))
                .withSort(Sort.by(Sort.Direction.DESC, "viewCount"))
                .withPageable(PageRequest.of(0, limit))
                .build();
    }

    // ==========================================
    // [가로 피드] 연관 영상 (More Like This / Terms)
    // ==========================================
    public NativeQuery buildRelatedQuery(List<FieldValue> tagValues, Long currentVideoId, int page, int limit) {
        Query relatedQuery = Query.of(q -> q.bool(b -> b
            .must(m -> m.terms(t -> t.field("tags").terms(tf -> tf.value(tagValues))))
            .mustNot(mn -> mn.term(t -> t.field("_id").value(currentVideoId.toString())))
            .filter(f -> f.term(t -> t.field("deleted").value(false)))
            .filter(f -> f.term(t -> t.field("videoType").value(videoType.name())))
        ));

        return NativeQuery.builder()
                .withQuery(relatedQuery)
                .withSort(Sort.by(Sort.Direction.DESC, "viewCount"))
                .withPageable(PageRequest.of(page, limit))
                .build();
    }


    // [세로 피드: 랜덤] 엘라스틱서치 random_score 쿼리
    public NativeQuery buildRandomQuery(VideoType videoType, int limit) {
        Query randomQuery = FunctionScoreQuery.of(fsq -> fsq
                .query(baseActiveVideoQuery(videoType))
                .functions(FunctionScore.of(f -> f.randomScore(rs -> rs)))
        )._toQuery();

        return NativeQuery.builder()
                .withQuery(randomQuery)
                .withPageable(PageRequest.of(0, limit))
                .build();
    }
}