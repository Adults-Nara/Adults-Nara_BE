package com.ott.core.modules.search.controller;

import com.ott.common.response.ApiResponse;
import com.ott.core.docs.RecommendationApiDocs;
import com.ott.core.modules.search.document.VideoDocument;
import com.ott.core.modules.search.dto.VideoFeedResponseDto;
import com.ott.core.modules.search.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController implements RecommendationApiDocs {
    private final RecommendationService recommendationService;

    @Override
    @GetMapping("/feed")
    public ResponseEntity<ApiResponse<List<VideoFeedResponseDto>>> getFeed(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        List<VideoDocument> rawDocuments = recommendationService.getPersonalizedFeed(userId, page, size);

        // Document -> DTO 로 변환
        List<VideoFeedResponseDto> dtoList = rawDocuments.stream()
                .map(VideoFeedResponseDto::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(dtoList));
    }
}
