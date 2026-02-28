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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "포인트 API", description = "사용자 포인트 조회, 내역 확인 및 적립 관련 API")

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/point")
public class PointController {
    private final PointService pointService;

    @Operation(summary = "사용자 포인트 잔액 조회", description = "현재 인증된 사용자의 가용 포인트 밸런스를 조회합니다.")
    @GetMapping
    public ApiResponse<UserPointBalanceResponse> getMyPointBalance(
            @AuthenticationPrincipal String userId) {
        return ApiResponse.success(pointService.findUserCurrentPoint(Long.parseLong(userId)));
    }

    @Operation(summary = "사용자 포인트 이력 조회", description = "사용자의 지난 포인트 적립/차감/사용 내역을 최신순으로 조회합니다.")
    @GetMapping("/history")
    public ApiResponse<List<PointTransactionHistoryResponse>> getMyPointTransactionHistory(
            @AuthenticationPrincipal String userId,
            PointTransactionHistoryRequest req) {
        return ApiResponse.success(pointService.findUserPointHistory(Long.parseLong(userId), req));
    }

    @Operation(summary = "상품 구매 보상 포인트 적립 (보너스 지급)", description = "상품을 구매한 사용자에게 거래 내역에 따른 보상 포인트를 즉시 적립합니다.")
    @PostMapping("/reward/purchase")
    public ApiResponse<Void> rewardPurchasePoint(
            @AuthenticationPrincipal String userId,
            @RequestBody ProductPurchaseRequest request) {

        pointService.rewardPurchaseBonus(Long.parseLong(userId), request);

        return ApiResponse.success(null);
    }
}
