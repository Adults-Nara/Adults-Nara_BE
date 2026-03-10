package com.ott.core.modules.uplus.controller;

import com.ott.common.response.ApiResponse;
import com.ott.core.modules.uplus.dto.UPlusSubscriptionDto;
import com.ott.core.modules.uplus.service.UPlusBillDiscountService;
import com.ott.core.modules.uplus.service.UPlusSubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/uplus")
@RequiredArgsConstructor
@Tag(name = "U+ 구독 API", description = "U+ 가입 정보 등록, 요금제 조회 및 포인트 할인 이력 API")
public class UPlusSubscriptionController {

    private final UPlusSubscriptionService subscriptionService;
    private final UPlusBillDiscountService billDiscountService;

    @Operation(summary = "요금제 목록 조회", description = "비로그인 사용자도 조회 가능한 U+ 요금제 목록을 반환합니다.")
    @GetMapping("/plans")
    public ApiResponse<List<UPlusSubscriptionDto.PlanInfo>> getPlans() {
        return ApiResponse.success(subscriptionService.getPlans());
    }

    @Operation(summary = "U+ 가입 정보 등록", description = "전화번호 입력 시 U+ API에서 가입 여부와 요금제를 자동 조회하여 등록합니다.")
    @PostMapping("/subscription")
    public ApiResponse<UPlusSubscriptionDto.RegisterResponse> register(
            @AuthenticationPrincipal String userId,
            @RequestBody @Valid UPlusSubscriptionDto.RegisterRequest request) {
        return ApiResponse.success(subscriptionService.register(Long.parseLong(userId), request));
    }

    @Operation(summary = "내 U+ 가입 정보 조회", description = "현재 로그인한 사용자의 U+ 가입 정보를 반환합니다.")
    @GetMapping("/subscription")
    public ApiResponse<UPlusSubscriptionDto.SubscriptionResponse> getMySubscription(
            @AuthenticationPrincipal String userId) {
        return ApiResponse.success(subscriptionService.getMySubscription(Long.parseLong(userId)));
    }

    @Operation(summary = "요금제 동기화", description = "U+ API를 재조회하여 최신 요금제로 업데이트합니다.")
    @PatchMapping("/subscription/plan")
    public ApiResponse<UPlusSubscriptionDto.SubscriptionResponse> syncPlan(
            @AuthenticationPrincipal String userId) {
        return ApiResponse.success(subscriptionService.syncPlan(Long.parseLong(userId)));
    }

    @Operation(summary = "U+ 구독 해지", description = "U+ 가입 정보를 해지(soft delete) 처리합니다.")
    @DeleteMapping("/subscription")
    public ApiResponse<Void> deactivate(@AuthenticationPrincipal String userId) {
        subscriptionService.deactivate(Long.parseLong(userId));
        return ApiResponse.success(null);
    }

    @Operation(summary = "포인트 할인 이력 조회", description = "현재 로그인한 사용자의 U+ 포인트 할인 이력을 최근 순으로 반환합니다.")
    @GetMapping("/discount/history")
    public ApiResponse<List<UPlusSubscriptionDto.DiscountHistoryResponse>> getDiscountHistory(
            @AuthenticationPrincipal String userId) {
        return ApiResponse.success(billDiscountService.getHistory(Long.parseLong(userId)));
    }
}