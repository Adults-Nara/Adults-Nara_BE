package com.ott.core.modules.recommendation.controller;

import com.ott.common.response.ApiResponse;
import com.ott.core.docs.RecommendationApiDocs;

import com.ott.core.modules.search.document.VideoDocument;
import com.ott.core.modules.recommendation.dto.SliceResponse;
import com.ott.core.modules.recommendation.dto.VideoFeedResponseDto;
import com.ott.core.modules.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
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

    private Long parseUserIdSafely(String userId) {
        if (userId == null || "anonymousUser".equals(userId)) {
            return null;
        }
        return Long.parseLong(userId);
    }
    // ==========================================
    // 메인 홈 피드
    // ==========================================
    @Override
    @GetMapping("/feed")
    public ApiResponse<SliceResponse<VideoFeedResponseDto>> getFeed(
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long parsedUserId = parseUserIdSafely(userId);
        List<VideoFeedResponseDto> dtoList = recommendationService.getPersonalizedFeed(parsedUserId, page, size);
        boolean hasNext = dtoList.size() == size;
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
        Long parsedUserId = parseUserIdSafely(userId);
        List<VideoFeedResponseDto> dtoList = recommendationService.getVerticalMixedFeed(parsedUserId, size);

        boolean hasNext = !dtoList.isEmpty();
        SliceResponse<VideoFeedResponseDto> sliceResponse = SliceResponse.of(dtoList, 0, size, hasNext);

        return ApiResponse.success(sliceResponse);
    }

    // ==========================================
    // 가로 연관 피드
    // ==========================================
    @Override
    @GetMapping("/{videoId}/related")
    public ApiResponse<SliceResponse<VideoFeedResponseDto>> getRelatedFeed(
            @AuthenticationPrincipal String userId,
            @PathVariable("videoId") Long videoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long parsedUserId = parseUserIdSafely(userId);

        // ✅ Document가 아닌 DTO 리스트를 반환받고, parsedUserId를 서비스로 넘김
        List<VideoFeedResponseDto> dtoList = recommendationService.getHorizontalRelatedVideos(videoId, parsedUserId, page, size);

        boolean hasNext = dtoList.size() == size;
        SliceResponse<VideoFeedResponseDto> sliceResponse = SliceResponse.of(dtoList, page, size, hasNext);

        return ApiResponse.success(sliceResponse);
    }
}
