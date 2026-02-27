package com.ott.core.modules.search.controller;

import com.ott.common.response.ApiResponse;
import com.ott.core.docs.RecommendationApiDocs;

import com.ott.core.modules.search.document.VideoDocument;
import com.ott.core.modules.search.dto.SliceResponse;
import com.ott.core.modules.search.dto.VideoFeedResponseDto;
import com.ott.core.modules.search.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
@Validated
public class RecommendationController implements RecommendationApiDocs {
    private final RecommendationService recommendationService;

    // ==========================================
    // 메인 홈 피드 (/api/v1/recommendations/feed)
    // ==========================================
    @Override
    @GetMapping("/feed")
    public ApiResponse<SliceResponse<VideoFeedResponseDto>> getFeed(
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        List<VideoDocument> rawDocuments = recommendationService.getPersonalizedFeed((userId != null) ? Long.parseLong(userId) : null, page, size);

        // Document -> DTO 로 변환
        List<VideoFeedResponseDto> dtoList = rawDocuments.stream()
                .map(VideoFeedResponseDto::from)
                .toList();

        // 다음 페이지 여부 (가져온 데이터가 요청한 사이즈와 같으면 다음 데이터가 있을 확률이 높음)
        boolean hasNext = rawDocuments.size() == size;
        SliceResponse<VideoFeedResponseDto> response = SliceResponse.of(dtoList, page, size, hasNext);

        return ApiResponse.success(response);
    }
    // ==========================================
    // 세로 믹스 피드
    // ==========================================
    @Override
    @GetMapping("/feed/vertical")
    public ApiResponse<SliceResponse<VideoFeedResponseDto>> getVerticalFeed(
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "10") int size
    ) {
        List<VideoDocument> rawDocuments = recommendationService.getVerticalMixedFeed((userId != null) ? Long.parseLong(userId) : null, size);

        List<VideoFeedResponseDto> dtoList = rawDocuments.stream()
                .map(VideoFeedResponseDto::from)
                .toList();

        // 릴스는 무한 스크롤이므로 true를 줘서 프론트가 계속 요청하게 유도
        boolean hasNext = !rawDocuments.isEmpty();
        SliceResponse<VideoFeedResponseDto> sliceResponse = SliceResponse.of(dtoList, 0, size, hasNext);

        return ApiResponse.success(sliceResponse);
    }

    // ==========================================
    // 가로 연관 피드
    // ==========================================
    @Override
    @GetMapping("/{videoId}/related")
    public ApiResponse<List<VideoFeedResponseDto>> getRelatedFeed(
            @PathVariable("videoId") Long videoId,
            @RequestParam(defaultValue = "10") int size
    ) {
        List<VideoDocument> rawDocuments = recommendationService.getHorizontalRelatedVideos(videoId, size);

        List<VideoFeedResponseDto> dtoList = rawDocuments.stream()
                .map(VideoFeedResponseDto::from)
                .toList();

        // 가로 스와이프는 보통 10~20개로 고정되어 끝나는 경우가 많아 일반 List
        return ApiResponse.success(dtoList);
    }
}
