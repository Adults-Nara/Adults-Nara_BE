package com.ott.core.modules.uplus.controller;

import com.ott.common.error.BusinessException;
import com.ott.common.error.ErrorCode;
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
@Tag(name = "U+ 구독 API", description = "U+ 가입 정보 연동 및 포인트 할인 이력 조회 API")
public class UPlusSubscriptionController {

    private final UPlusSubscriptionService subscriptionService;
    private final UPlusBillDiscountService billDiscountService;

    @Operation(
            summary = "U+ 가입 정보 확인",
            description = "전화번호를 입력하면 U+ 가입 여부를 확인합니다. " +
                    "가입되지 않은 번호이거나 본인 명의가 아닌 경우 오류가 반환됩니다."
    )
    @PostMapping("/verify")
    public ApiResponse<UPlusSubscriptionDto.LinkResponse> verify(
            @AuthenticationPrincipal String userId,
            @RequestBody @Valid UPlusSubscriptionDto.LinkRequest request) {
        long parsedUserId;
        try {
            parsedUserId = Long.parseLong(userId);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return ApiResponse.success(subscriptionService.verify(parsedUserId, request));
    }

    @Operation(
            summary = "내 U+ 가입 정보 조회",
            description = "현재 로그인한 사용자의 U+ 가입 정보(요금제, 활성 여부)를 반환합니다."
    )
    @GetMapping("/subscription")
    public ApiResponse<UPlusSubscriptionDto.SubscriptionResponse> getMySubscription(
            @AuthenticationPrincipal String userId) {
        long parsedUserId;
        try {
            parsedUserId = Long.parseLong(userId);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return ApiResponse.success(subscriptionService.getMySubscription(parsedUserId));
    }

    @Operation(
            summary = "U+ 포인트 할인 이력 조회",
            description = "현재 로그인한 사용자의 U+ 포인트 할인 이력을 최근 순으로 반환합니다."
    )
    @GetMapping("/discount/history")
    public ApiResponse<List<UPlusSubscriptionDto.DiscountHistoryResponse>> getDiscountHistory(
            @AuthenticationPrincipal String userId) {
        long parsedUserId;
        try {
            parsedUserId = Long.parseLong(userId);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return ApiResponse.success(billDiscountService.getHistory(parsedUserId));
    }
}