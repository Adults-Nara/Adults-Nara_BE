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

    private void applyBaseActiveVideoFilters(BoolQuery.Builder b, VideoType videoType, List<String> excludedVideoIds) {
        b.filter(f -> f.term(t -> t.field("deleted").value(false)))
                .filter(f -> f.term(t -> t.field("videoType").value(videoType.name())))
                .filter(f -> f.term(t -> t.field("isAd").value(false)));

        if (excludedVideoIds != null && !excludedVideoIds.isEmpty()) {
            List<FieldValue> values = excludedVideoIds.stream().map(FieldValue::of).toList();
            b.mustNot(mn -> mn.terms(t -> t.field("_id").terms(tf -> tf.value(values))));
        }
    }

    private Query baseActiveVideoQuery(VideoType videoType, List<String> excludedVideoIds) {
        return Query.of(q -> q.bool(b -> {
            applyBaseActiveVideoFilters(b, videoType, excludedVideoIds);
            return b;
        }));
    }

    // ==========================================
    // [메인 피드용] 유저 취향 벡터 기반 kNN 쿼리
    // ==========================================
    public NativeQuery buildMainPersonalizedKnnQuery(List<Float> userVector, VideoType videoType, int page, int size, List<String> excludedVideoIds) {
        // 벡터가 없으면 Fallback 쿼리 반환 로직을 Service에서 처리
        int cappedSize = Math.min(size, 100); // 보안 방어 로직: size 상한선을 100으로 제한
        // kNN 쿼리 내부에 deleted=false 필터 결합
        Query filterQuery = baseActiveVideoQuery(videoType, excludedVideoIds);
        // 후보군 넉넉히 잡되, DoS 방지를 위해 최대 500으로 제한
        int numCandidates = Math.min(Math.max(50, cappedSize * 5), 500);

        Query knnQuery = Query.of(q -> q
                .knn(k -> k
                        .field("embedding")
                        .queryVector(userVector)
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
    // 2. 사용자 선호도별 추천
    // ==========================================
    public NativeQuery buildMainPersonalizedQuery(List<TagScoreDto> userPreferences, VideoType videoType, int page, int size, List<String> excludedVideoIds) {
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
                .query(baseActiveVideoQuery(videoType, excludedVideoIds))
                .functions(functions)
                .scoreMode(FunctionScoreMode.Sum)
                .boostMode(FunctionBoostMode.Replace)
        )._toQuery();

        return NativeQuery.builder()
                .withQuery(functionScoreQuery)
                .withPageable(PageRequest.of(page, size))
                .build();
    }
    // ==========================================
    // 2. [메인 피드용] 신규 유저 Fallback 쿼리
    // ==========================================
    public NativeQuery buildFallbackQuery(VideoType videoType, int page, int size, List<String> excludedVideoIds) {
        return NativeQuery.builder()
                .withQuery(baseActiveVideoQuery(videoType, excludedVideoIds))
                .withSort(Sort.by(Sort.Direction.DESC, "viewCount")) // 1순위: 인기순
                .withSort(Sort.by(Sort.Direction.DESC, "createdAt")) // 2순위: 최신순
                .withPageable(PageRequest.of(page, size))
                .build();
    }

    // 세로 피드 (20%): 인기순 쿼리
    public NativeQuery buildPopularQuery(VideoType videoType, int page, int limit, List<String> excludedVideoIds) {
        return NativeQuery.builder()
                .withQuery(baseActiveVideoQuery(videoType, excludedVideoIds))
                .withSort(Sort.by(Sort.Direction.DESC, "viewCount"))
                .withPageable(PageRequest.of(page, limit))
                .build();
    }

    public NativeQuery buildRandomQuery(VideoType videoType, int page, int limit, List<String> excludedVideoIds, Long userId) {
        String randomSeed = userId + "_" + java.time.LocalDate.now().toString();

        Query randomQuery = FunctionScoreQuery.of(fsq -> fsq
                .query(baseActiveVideoQuery(videoType, excludedVideoIds))
                .functions(FunctionScore.of(f -> f.randomScore(rs -> rs
                        .seed(randomSeed)
                        .field("_seq_no")
                )))
        )._toQuery();

        return NativeQuery.builder()
                .withQuery(randomQuery)
                .withPageable(PageRequest.of(page, limit))
                .build();
    }
    // ==========================================
    // [가로 피드] 연관 영상 (More Like This / Terms)
    // ==========================================
    public NativeQuery buildRelatedQuery(List<FieldValue> tagValues, VideoType videoType, int page, int limit, List<String> excludedVideoIds) {

        Query relatedQuery = Query.of(q -> q.bool(b -> {
            applyBaseActiveVideoFilters(b, videoType, excludedVideoIds);
            b.must(m -> m.terms(t -> t.field("tags").terms(tf -> tf.value(tagValues))));
            return b;
        }));

        return NativeQuery.builder()
                .withQuery(relatedQuery)
                .withSort(Sort.by(Sort.Direction.DESC, "viewCount"))
                .withPageable(PageRequest.of(page, limit))
                .build();
    }

    // ==========================================
    // [세로 피드] 유저 취향(태그) 맞춤형 타겟팅 광고 쿼리
    // ==========================================
    public NativeQuery buildPersonalizedAdQuery(List<TagScoreDto> userPreferences, VideoType videoType) {

        // 1. 베이스 조건: 삭제 안 됨 + 해당 비디오 타입 + 광고(isAd = true)
        Query baseAdQuery = Query.of(q -> q.bool(b -> b
                .filter(f -> f.term(t -> t.field("deleted").value(false)))
                .filter(f -> f.term(t -> t.field("videoType").value(videoType.name())))
                .filter(f -> f.term(t -> t.field("isAd").value(true)))
        ));

        List<FunctionScore> functions = new ArrayList<>();

        // 2. 가중치 1: 유저의 태그 선호도 점수 반영 (타겟팅 핵심 로직)
        if (userPreferences != null && !userPreferences.isEmpty()) {
            for (TagScoreDto pref : userPreferences) {
                if (pref.score() <= 0) continue;
                functions.add(FunctionScore.of(f -> f
                        .filter(fq -> fq.term(t -> t.field("tags").value(pref.tagName())))
                        .weight(pref.score()) // 취향 점수가 높을수록 해당 태그를 가진 광고가 위로 올라옴
                ));
            }
        }

        // 3. 가중치 2: 광고 피로도 방지를 위한 약간의 랜덤 스코어 추가
        functions.add(FunctionScore.of(f -> f.randomScore(rs -> rs).weight(0.5)));

        // 4. 스코어 조합
        Query targetedAdQuery = FunctionScoreQuery.of(fsq -> fsq
                .query(baseAdQuery)
                .functions(functions)
                .scoreMode(FunctionScoreMode.Sum) // 태그 점수들을 합산
                .boostMode(FunctionBoostMode.Replace) 
        )._toQuery();

        return NativeQuery.builder()
                .withQuery(targetedAdQuery)
                .withPageable(PageRequest.of(0, 1)) // 가장 점수가 높은 타겟팅 광고 딱 1개만 추출
                .build();
    }
}