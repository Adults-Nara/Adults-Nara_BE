package com.ott.core.modules.search.controller;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.ott.common.response.ApiResponse;
import com.ott.core.docs.SearchAdminApiDocs;
import com.ott.core.modules.search.document.VideoDocument;
import com.ott.core.modules.search.service.VideoSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/search/admin")
@RequiredArgsConstructor
public class SearchAdminController implements SearchAdminApiDocs {

    private final VideoSyncService videoSyncService;
    private final ElasticsearchOperations elasticsearchOperations;
    @Override
    @PostMapping("/sync")
    public ApiResponse<String> syncData() {
        videoSyncService.syncAllVideosToElasticsearch();
        String message = "✅ 엘라스틱서치 데이터 동기화가 백그라운드에서 완료되었습니다. 콘솔 로그를 확인하세요!";
        return ApiResponse.success(message);
    }

    @PostMapping("/clear-index")
    public ApiResponse<String> clearSearchIndex() {
        log.warn("🚨 관리자에 의해 ES 비디오 인덱스 초기화가 시작되었습니다!");

        // MatchAll 쿼리 생성
        Query matchAllQuery = Query.of(q -> q.matchAll(ma -> ma));
        NativeQuery deleteQuery = NativeQuery.builder().withQuery(matchAllQuery).build();

        // 쿼리에 해당하는 모든 문서 삭제 (인덱스는 유지됨)
        elasticsearchOperations.delete(deleteQuery, VideoDocument.class);

        log.info("✅ ES 비디오 인덱스 데이터가 100% 삭제되었습니다.");

        return ApiResponse.success("엘라스틱서치 비디오 데이터가 성공적으로 싹 비워졌습니다.");
    }
}
