package com.ott.core.modules.point.controller;

import com.ott.common.response.ApiResponse;
import com.ott.core.modules.point.dto.PointTransactionHistoryRequest;
import com.ott.core.modules.point.dto.PointTransactionHistoryResponse;
import com.ott.core.modules.point.dto.ProductPurchaseRequest;
import com.ott.core.modules.point.dto.UserPointBalanceResponse;
import com.ott.core.modules.point.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/point")
public class PointController {
    private final PointService pointService;

    @GetMapping
    public ApiResponse<UserPointBalanceResponse> getMyPointBalance(
            @AuthenticationPrincipal String userId) {
        return ApiResponse.success(pointService.findUserCurrentPoint(Long.parseLong(userId)));
    }

    @GetMapping("/history")
    public ApiResponse<List<PointTransactionHistoryResponse>> getMyPointTransactionHistory(
            @AuthenticationPrincipal String userId,
            PointTransactionHistoryRequest req) {
        return ApiResponse.success(pointService.findUserPointHistory(Long.parseLong(userId), req));
    }

    @PostMapping("/reward/purchase")
    public ApiResponse<Void> rewardPurchasePoint(
            @AuthenticationPrincipal String userId,
            @RequestBody ProductPurchaseRequest request) {

        pointService.rewardPurchaseBonus(Long.parseLong(userId), request);

        return ApiResponse.success(null);
    }
}
