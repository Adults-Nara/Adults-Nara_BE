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
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(pointService.findUserCurrentPoint(userId));
    }

    @GetMapping("/details")
    public ApiResponse<List<PointTransactionHistoryResponse>> getMyPointTransactionHistory(
            @AuthenticationPrincipal Long userId,
            PointTransactionHistoryRequest req) {
        return ApiResponse.success(pointService.findUserPointHistory(userId, req));
    }

    @PostMapping("/purchase/reward")
    public ApiResponse<Void> rewardPurchasePoint(
            @AuthenticationPrincipal Long userId,
            @RequestBody ProductPurchaseRequest request) {

        // 내부적으로 DuplicateKeyException 발생 시 멱등성을 지키기 위해 BusinessException으로 차단됨
        pointService.rewardPurchaseReward(userId, request);

        return ApiResponse.success(null);
    }
}
