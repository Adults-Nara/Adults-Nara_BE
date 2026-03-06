package com.ott.core.modules.bookmark.controller;

import com.ott.common.response.ApiResponse;
import com.ott.core.modules.bookmark.dto.RankingResponse;
import com.ott.core.modules.bookmark.service.RankingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "랭킹 API", description = "실시간 북마크(찜) 기반 인기 영상 랭킹 기능")
@RestController
@RequestMapping("/api/v1/ranking")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    @Operation(summary = "실시간 인기 영상 랭킹 조회", description = "Redis에 캐싱된 북마크 개수를 기준으로 가장 인기 있는 영상들의 랭킹 리스트를 반환합니다.")
    @GetMapping
    public ApiResponse<List<RankingResponse>> getBookmarkRanking(
            @Parameter(description = "조회할 랭킹 개수 (기본값: 10, 최대 노출 개수 지정)", example = "10")
            @RequestParam(name = "limit", defaultValue = "10") int limit) {

        List<RankingResponse> rankingList = rankingService.getTopBookmarkVideos(limit);

        return ApiResponse.success(rankingList);
    }
}

