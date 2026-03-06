package com.ott.core.modules.uplus.controller;

import com.ott.common.response.ApiResponse;
import com.ott.core.modules.uplus.dto.UPlusSubscriptionDto;
import com.ott.core.modules.uplus.service.UPlusBillDiscountService;
import com.ott.core.modules.uplus.service.UPlusSubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/uplus")
@RequiredArgsConstructor
public class UPlusSubscriptionController {

    private final UPlusSubscriptionService subscriptionService;
    private final UPlusBillDiscountService billDiscountService;

    /**
     * GET /api/v1/uplus/plans
     * 요금제 목록 (비로그인 접근 가능)
     */
    @GetMapping("/plans")
    public ApiResponse<List<UPlusSubscriptionDto.PlanInfo>> getPlans() {
        return ApiResponse.success(subscriptionService.getPlans());
    }

    /**
     * POST /api/v1/uplus/subscription
     * U+ 가입 정보 등록
     *
     * 전화번호만 입력 → U+ API에서 가입 여부 + 요금제 자동 조회 후 등록
     * - 비가입자: registered=false + 안내 메시지 (DB 저장 없음)
     * - 가입자:   registered=true + 가입 정보 반환 (요금제 자동 세팅)
     */
    @PostMapping("/subscription")
    public ApiResponse<UPlusSubscriptionDto.RegisterResponse> register(
            @AuthenticationPrincipal String userId,
            @RequestBody @Valid UPlusSubscriptionDto.RegisterRequest request) {
        return ApiResponse.success(subscriptionService.register(Long.parseLong(userId), request));
    }

    /**
     * GET /api/v1/uplus/subscription
     * 내 U+ 가입 정보 조회
     */
    @GetMapping("/subscription")
    public ApiResponse<UPlusSubscriptionDto.SubscriptionResponse> getMySubscription(
            @AuthenticationPrincipal String userId) {
        return ApiResponse.success(subscriptionService.getMySubscription(Long.parseLong(userId)));
    }

    /**
     * PATCH /api/v1/uplus/subscription/plan
     * 요금제 동기화 (U+ API 재조회해서 최신 요금제로 업데이트)
     */
    @PatchMapping("/subscription/plan")
    public ApiResponse<UPlusSubscriptionDto.SubscriptionResponse> syncPlan(
            @AuthenticationPrincipal String userId) {
        return ApiResponse.success(subscriptionService.syncPlan(Long.parseLong(userId)));
    }

    /**
     * DELETE /api/v1/uplus/subscription
     * 해지 (soft delete)
     */
    @DeleteMapping("/subscription")
    public ApiResponse<Void> deactivate(@AuthenticationPrincipal String userId) {
        subscriptionService.deactivate(Long.parseLong(userId));
        return ApiResponse.success(null);
    }

    /**
     * GET /api/v1/uplus/discount/history
     * 내 U+ 포인트 할인 이력 (최근 순)
     */
    @GetMapping("/discount/history")
    public ApiResponse<List<UPlusSubscriptionDto.DiscountHistoryResponse>> getDiscountHistory(
            @AuthenticationPrincipal String userId) {
        return ApiResponse.success(billDiscountService.getHistory(Long.parseLong(userId)));
    }
}