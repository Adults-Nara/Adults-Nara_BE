package com.ott.core.modules.uplus.controller;

import com.ott.common.error.BusinessException;
import com.ott.common.error.ErrorCode;
import com.ott.common.response.ApiResponse;
import com.ott.core.modules.uplus.dto.UPlusSubscriptionDto;
import com.ott.core.modules.uplus.service.UPlusBillDiscountService;
import com.ott.core.modules.uplus.service.UPlusSubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
                    "모든 실패 케이스(형식 오류, 미존재, 본인 명의 불일치, 해지)는 verified=false로 반환됩니다."
    )
    @PostMapping("/verify")
    public ApiResponse<UPlusSubscriptionDto.LinkResponse> verify(
            @AuthenticationPrincipal String userId,
            @RequestBody UPlusSubscriptionDto.LinkRequest request) {  // @Valid 제거
        return ApiResponse.success(subscriptionService.verify(parseUserId(userId), request));
    }

    @Operation(
            summary = "내 U+ 가입 정보 조회",
            description = "현재 로그인한 사용자의 U+ 가입 정보(요금제, 활성 여부)를 반환합니다."
    )
    @GetMapping("/subscription")
    public ApiResponse<UPlusSubscriptionDto.SubscriptionResponse> getMySubscription(
            @AuthenticationPrincipal String userId) {
        return ApiResponse.success(subscriptionService.getMySubscription(parseUserId(userId)));
    }

    @Operation(
            summary = "U+ 포인트 할인 이력 조회",
            description = "현재 로그인한 사용자의 U+ 포인트 할인 이력을 최근 순으로 반환합니다."
    )
    @GetMapping("/discount/history")
    public ApiResponse<List<UPlusSubscriptionDto.DiscountHistoryResponse>> getDiscountHistory(
            @AuthenticationPrincipal String userId) {
        return ApiResponse.success(billDiscountService.getHistory(parseUserId(userId)));
    }

    // ====== Private Methods ======

    private long parseUserId(String userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, e);
        }
    }
}