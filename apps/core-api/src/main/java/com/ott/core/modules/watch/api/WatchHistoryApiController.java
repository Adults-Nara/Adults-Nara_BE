package com.ott.core.modules.watch.api;

import com.ott.common.response.ApiResponse;
import com.ott.core.modules.watch.dto.request.WatchPositionRequest;
import com.ott.core.modules.watch.dto.response.WatchHistoryPageResponse;
import com.ott.core.modules.watch.dto.response.WatchHistoryResponse;
import com.ott.core.modules.watch.service.WatchHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/watch")
public class WatchHistoryApiController {

    private final WatchHistoryService watchHistoryService;

    // 시청 위치 조회 (영상 재생 시작 전 호출)
    @GetMapping("/{videoId}")
    public ApiResponse<WatchHistoryResponse> getWatchHistory(
            @AuthenticationPrincipal String userId,
            @PathVariable("videoId") Long videoId
    ) {
        WatchHistoryResponse response = watchHistoryService.getWatchHistory(Long.parseLong(userId), videoId);
        return ApiResponse.success(response);
    }

    // 영상 재생 중 시청 위치 업데이트 (10초마다 클라이언트에서 호출)
    @PatchMapping("/{videoId}/position")
    public ApiResponse<?> updateWatchPosition(
            @AuthenticationPrincipal String userId,
            @PathVariable("videoId") Long videoId,
            @RequestBody WatchPositionRequest request
    ) {
        watchHistoryService.updateWatchPosition(Long.parseLong(userId), videoId, request.getLastPosition(), request.getDuration());
        return ApiResponse.success();
    }

    // 영상 시청 종료 시 최종 위치 저장
    @PostMapping("/{videoId}/stop")
    public ApiResponse<?> stopWatching(
            @AuthenticationPrincipal String userId,
            @PathVariable("videoId") Long videoId,
            @RequestBody WatchPositionRequest request
    ) {
        watchHistoryService.stopWatching(Long.parseLong(userId), videoId, request.getLastPosition(), request.getDuration());
        return ApiResponse.success();
    }

    // 최근 3개월간 시청 이력 조회
    @GetMapping("/history/recent")
    public ApiResponse<WatchHistoryPageResponse> getRecentWatchHistory(
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        WatchHistoryPageResponse response = watchHistoryService.getRecentWatchHistory(Long.parseLong(userId), page, size);
        return ApiResponse.success(response);
    }
}
