package com.ott.core.docs;

import com.ott.core.modules.search.dto.VideoFeedResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "Recommendation API", description = "사용자 맞춤형 추천 피드 및 영상 탐색 API")
public interface RecommendationApiDocs {

    @Operation(
            summary = "메인 홈 맞춤형 추천 피드 조회",
            description = """
                    사용자의 시청 이력(Redis 취향 점수)을 기반으로 Elasticsearch 기반 개인화 추천 목록을 반환합니다.<br>
                    - <b>기존 유저:</b> 내 선호 태그 가중치 + 인기(조회수) 종합 점수순 정렬<br>
                    - <b>신규 유저:</b> 인기 및 최신순 기본 피드 제공 (Fallback)
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "추천 피드 조회 성공 (데이터가 없으면 빈 배열 반환)")
    })
    ResponseEntity<com.ott.common.response.ApiResponse<List<VideoFeedResponseDto>>> getFeed(
            @Parameter(description = "조회할 사용자의 고유 ID", example = "9999", required = true)
            Long userId,

            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            int page,

            @Parameter(description = "한 페이지당 가져올 비디오 개수", example = "10")
            int size
    );
}