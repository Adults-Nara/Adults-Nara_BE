package com.ott.core.docs;

import com.ott.common.response.ApiResponse; // 우리가 만든 커스텀 응답 객체
import com.ott.core.modules.search.dto.SliceResponse;
import com.ott.core.modules.search.dto.VideoFeedResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "Recommendation API", description = "사용자 맞춤형 추천 피드 및 영상 탐색 API")
public interface RecommendationApiDocs {

    // 메인 홈 피드
    @Operation(
            summary = "메인 홈 맞춤형 추천 피드 조회",
            description = """
                    사용자의 시청 이력(Redis 취향 점수)을 기반으로 Elasticsearch 기반 개인화 추천 목록을 반환합니다.<br>
                    - <b>기존 유저:</b> 내 선호 태그 가중치 + 인기(조회수) 종합 점수순 정렬<br>
                    - <b>신규 유저:</b> 인기 및 최신순 기본 피드 제공 (Fallback)
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "추천 피드 조회 성공 (데이터가 없으면 빈 배열 반환)")
    })
    ResponseEntity<ApiResponse<SliceResponse<VideoFeedResponseDto>>> getFeed(
            @Parameter(description = "조회할 사용자의 고유 ID", example = "9999", required = true)
            Long userId,

            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @Min(0) int page,

            @Parameter(description = "한 페이지당 가져올 비디오 개수", example = "10")
            @Min(1) @Max(50) int size
    );
    // 세로 스와이프 피드 (숏폼/릴스)
    @Operation(summary = "세로 믹스 피드 조회 (7:2:1)", description = "취향(70%), 인기(20%), 랜덤(10%) 비율로 섞인 지루하지 않은 피드를 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<ApiResponse<SliceResponse<VideoFeedResponseDto>>> getVerticalFeed(
            @Parameter(description = "사용자 ID") Long userId,
            @Parameter(description = "가져올 개수 (기본 10)") @Min(1) @Max(50) int size
    );

    // 가로 스와이프 피드 (상세페이지 연관 영상)
    @Operation(summary = "연관 영상 추천 조회", description = "현재 시청 중인 영상과 태그가 비슷한 연관 영상을 추천합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })

    ResponseEntity<ApiResponse<List<VideoFeedResponseDto>>> getRelatedFeed(
            @Parameter(description = "현재 영상의 물리적 ID") Long videoId,
            @Parameter(description = "가져올 개수 (기본 10)") @Min(1) @Max(20) int size
    );
}