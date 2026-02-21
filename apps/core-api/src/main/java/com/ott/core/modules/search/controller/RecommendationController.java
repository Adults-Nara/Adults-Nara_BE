package com.ott.core.modules.search.controller;

import com.ott.core.docs.RecommendationApiDocs;
import com.ott.core.modules.search.document.VideoDocument;
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
    public ResponseEntity<List<VideoDocument>> getFeed(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        List<VideoDocument> recommendedVideos = recommendationService.getPersonalizedFeed(userId, page, size);
        return ResponseEntity.ok(recommendedVideos);
    }
}
