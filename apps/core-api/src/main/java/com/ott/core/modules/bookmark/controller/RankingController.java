package com.ott.core.modules.bookmark.controller;

import com.ott.common.response.ApiResponse;
import com.ott.core.modules.bookmark.dto.RankingResponse;
import com.ott.core.modules.bookmark.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ranking")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RankingResponse>>> getBookmarkRanking(
        @RequestParam(name = "limit", defaultValue = "10") int limit) {

        List<RankingResponse> rankingList = rankingService.getTopBookmarkVideos(limit);

        return ResponseEntity.ok(ApiResponse.success(rankingList));
    }
}

