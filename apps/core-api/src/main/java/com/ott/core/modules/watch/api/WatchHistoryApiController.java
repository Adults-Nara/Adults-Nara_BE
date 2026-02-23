package com.ott.core.modules.watch.api;

import com.ott.common.response.ApiResponse;
import com.ott.core.modules.watch.dto.request.WatchPositionRequest;
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
    @GetMapping("/{videoMetadataId}")
    public ApiResponse<WatchHistoryResponse> getWatchHistory(
            @AuthenticationPrincipal String userId,
            @PathVariable("videoMetadataId") Long videoMetadataId
    ) {
        long userIdLong = Long.parseLong(userId);
        System.out.println("userId: " + userId);
        System.out.println("userIdLong = " + userIdLong);
        WatchHistoryResponse response = watchHistoryService.getWatchHistory(userIdLong, videoMetadataId);
        return ApiResponse.success(response);
    }

    // 영상 재생 중 시청 위치 업데이트 (10초마다 클라이언트에서 호출)
    @PatchMapping("/{videoMetadataId}/position")
    public ApiResponse<?> updateWatchPosition(
            @AuthenticationPrincipal String userId,
            @PathVariable("videoMetadataId") Long videoMetadataId,
            @RequestBody WatchPositionRequest request
    ) {
        long userIdLong = Long.parseLong(userId);
        watchHistoryService.updateWatchPosition(userIdLong, videoMetadataId, request.getLastPosition(), request.getDuration());
        return ApiResponse.success();
    }

    // 영상 시청 종료 시 최종 위치 저장
    @PostMapping("/{videoMetadataId}/stop")
    public ApiResponse<?> stopWatching(
            @AuthenticationPrincipal String userId,
            @PathVariable("videoMetadataId") Long videoMetadataId,
            @RequestBody WatchPositionRequest request
    ) {
        long userIdLong = Long.parseLong(userId);
        watchHistoryService.stopWatching(userIdLong, videoMetadataId, request.getLastPosition(), request.getDuration());
        return ApiResponse.success();
    }
}
