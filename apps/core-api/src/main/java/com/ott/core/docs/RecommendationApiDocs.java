package com.ott.core.docs;

import com.ott.common.persistence.enums.VideoType;
import com.ott.common.response.ApiResponse;
import com.ott.core.modules.recommendation.dto.SliceResponse;
import com.ott.core.modules.recommendation.dto.VideoFeedResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Tag(name = "Recommendation API", description = "사용자 맞춤형 추천 피드 및 영상 탐색 API")
public interface RecommendationApiDocs {

    // 메인 홈 피드
    @Operation(
            summary = "메인 홈 맞춤형 추천 피드 조회",
            description = """
                    사용자의 시청 이력(Redis 취향 점수)을 기반으로 Elasticsearch 기반 개인화 추천 목록을 반환합니다.<br>
                    - <b>기존 유저:</b> 내 선호 태그 가중치 + 인기(조회수) 종합 점수순 정렬<br>
                    - <b>신규 유저:</b> 인기 및 최신순 기본 피드 제공 (Fallback)<br>
                    - <b>타입 필터링:</b> videoType 파라미터를 넘기면 해당 타입(SHORT/LONG)만 반환하며, 안 넘기면 전체 조회됩니다.
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "추천 피드 조회 성공 (데이터가 없으면 빈 배열 반환)")
    })
    ApiResponse<SliceResponse<VideoFeedResponseDto>> getFeed(
            @Parameter(description = "조회할 사용자의 고유 ID", example = "9999", required = true)
            String userId,

            @Parameter(description = "영상 타입 필터링 (SHORT 또는 LONG). 비워두면 모든 영상 조회", example = "LONG")
            VideoType videoType,

            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @Min(0) int page,

            @Parameter(description = "한 페이지당 가져올 비디오 개수", example = "10")
            @Min(1) @Max(50) int size
    );

    // 세로 스와이프 피드 (숏폼/릴스)
    // ✅ 수정: 광고 주입 내용 명시 및 page 파라미터 추가
    @Operation(
            summary = "세로 믹스 피드 조회 (무한 스크롤 및 광고 포함)",
            description = """
                    취향(70%), 인기(20%), 랜덤(10%) 비율로 섞인 지루하지 않은 피드를 반환합니다.<br>
                    - <b>무한 스크롤:</b> page 파라미터를 증가시키며 다음 피드를 요청할 수 있습니다.<br>
                    - <b>광고 주입(Ad Injection):</b> 사용자의 태그 취향에 맞춘 타겟팅 광고가 피드 중간(랜덤 위치)에 100% 확률(개발 환경 기준)로 1개 삽입됩니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ApiResponse<SliceResponse<VideoFeedResponseDto>> getVerticalFeed(
            @Parameter(description = "사용자 ID")
            String userId,

            @Parameter(description = "영상 타입 필터링 (주로 SHORT 사용)", example = "SHORT")
            VideoType videoType,

            @Parameter(description = "페이지 번호 (무한 스크롤용, 0부터 시작)", example = "0")
            @Min(0) int page,

            @Parameter(description = "가져올 개수 (기본 10)")
            @Min(1) @Max(50) int size
    );

    // 가로 스와이프 피드 (상세페이지 연관 영상)
    @Operation(summary = "연관 영상 추천 조회", description = "현재 시청 중인 영상과 태그가 비슷한 연관 영상을 추천합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ApiResponse<SliceResponse<VideoFeedResponseDto>> getRelatedFeed(
            @Parameter(description = "조회할 사용자의 고유 ID", example = "9999", required = true)
            String userId,

            @Parameter(description = "현재 영상의 물리적 ID")
            Long videoId,

            @Parameter(description = "영상 타입 필터링", example = "LONG")
            VideoType videoType,

            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @Min(0) int page,

            @Parameter(description = "가져올 개수 (기본 10)")
            @Min(1) @Max(20) int size
    );
}