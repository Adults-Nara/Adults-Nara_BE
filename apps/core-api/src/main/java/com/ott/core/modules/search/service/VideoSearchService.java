package com.ott.core.modules.search.service;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.ott.common.persistence.enums.VideoType;
import com.ott.core.modules.search.document.VideoDocument;
import com.ott.core.modules.search.dto.VideoSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VideoSearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    /**
     * 1. 메인 검색 API (형태소 분석 + 필터링 + 가중치)
     */
    public Page<VideoSearchResponse> searchVideos(String keyword, VideoType videoType, String tag, Pageable pageable) {

        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        // [필수 필터] 삭제되지 않은 영상만 노출
        boolQueryBuilder.filter(f -> f.term(t -> t.field("deleted").value(false)));

        // [MUST] 검색어 매칭 (제목 3배 가중치, 내용은 1배)
        if (keyword != null && !keyword.isBlank()) {
            boolQueryBuilder.must(m -> m
                    .multiMatch(match -> match
                            .query(keyword)
                            .fields("title^3.0", "description") // Nori 분석기가 작동하는 필드
                    )
            );
        }

        // [FILTER] 비디오 타입 필터 (예: SHORT, NORMAL) - 캐싱 적용되어 매우 빠름
        if (videoType != null) {
            boolQueryBuilder.filter(f -> f.term(t -> t.field("videoType").value(videoType.name())));
        }

        // [FILTER] 태그 필터 (정확히 일치하는 태그)
        if (tag != null && !tag.isBlank()) {
            boolQueryBuilder.filter(f -> f.term(t -> t.field("tags").value(tag)));
        }

        // 쿼리 조립
        Query query = Query.of(q -> q.bool(boolQueryBuilder.build()));

        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(query)
                .withPageable(pageable) // 페이징 처리
                .build();

        // ES에 쿼리 실행
        SearchHits<VideoDocument> searchHits = elasticsearchOperations.search(nativeQuery, VideoDocument.class);

        // 결과를 DTO로 변환
        List<VideoSearchResponse> content = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(VideoSearchResponse::from)
                .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, searchHits.getTotalHits());
    }

    /**
     * 2. 실시간 자동완성 API (Edge N-gram 활용)
     * 사용자가 타이핑할 때마다 즉각적으로 연관 검색어(제목) 5개를 내려줍니다.
     */
    public List<String> autocomplete(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        // title.ngram 필드를 대상으로 Match 쿼리 실행
        Query query = Query.of(q -> q
                .match(m -> m
                        .field("title.ngram") // 인덱스 매핑 때 만든 N-gram 서브 필드
                        .query(keyword)
                )
        );

        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(query)
                .withMaxResults(5) // 자동완성은 상위 5개만 빠르게
                .build();

        SearchHits<VideoDocument> searchHits = elasticsearchOperations.search(nativeQuery, VideoDocument.class);

        return searchHits.getSearchHits().stream()
                .map(hit -> hit.getContent().getTitle())
                .distinct() // 중복 제목 제거
                .collect(Collectors.toList());
    }
}