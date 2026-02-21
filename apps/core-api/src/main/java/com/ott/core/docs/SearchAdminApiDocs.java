package com.ott.core.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Search Admin API", description = "검색 엔진(Elasticsearch) 관리자 전용 API")
public interface SearchAdminApiDocs {

    @Operation(
            summary = "DB ➔ 엘라스틱서치 전체 데이터 동기화",
            description = """
                    RDBMS(PostgreSQL)에 저장된 모든 비디오 및 태그 메타데이터를 엘라스틱서치로 벌크 인덱싱(Bulk Indexing)합니다.<br>
                    - <b>안전장치:</b> 1,000건 단위 분할 처리(Paging) 및 N+1 쿼리 최적화 적용됨.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "동기화 성공 메시지 반환")
    })
    String syncData();
}