package com.ott.core.modules.uplus.dto;

import com.ott.common.persistence.entity.UPlusBillDiscount;
import com.ott.common.persistence.entity.UPlusSubscription;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class UPlusSubscriptionDto {

    // ===== Request =====

    /**
     * U+ 가입 정보 등록 요청
     * 전화번호만 입력 → U+ API에서 가입 여부 + 요금제 자동 조회 후 등록
     */
    @Getter
    public static class RegisterRequest {

        @NotBlank
        @Pattern(regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$",
                message = "올바른 휴대폰 번호를 입력해주세요.")
        private String phoneNumber;
    }

    // ===== Response =====

    /**
     * 등록 결과 응답
     * - registered=true  → 가입자 확인 및 등록 완료, 요금제 정보 포함
     * - registered=false → 비가입자, message만 표시하고 종료
     */
    @Getter
    @RequiredArgsConstructor
    public static class RegisterResponse {

        private final boolean registered;
        private final String message;
        private final SubscriptionResponse subscription; // 비가입자면 null

        public static RegisterResponse success(UPlusSubscription s) {
            return new RegisterResponse(
                    true,
                    "U+ 가입이 확인되어 할인 혜택이 등록되었습니다. 매월 1일 포인트가 자동으로 청구서 할인에 반영됩니다.",
                    SubscriptionResponse.from(s)
            );
        }

        public static RegisterResponse notSubscriber() {
            return new RegisterResponse(
                    false,
                    "U+ 가입 정보를 찾을 수 없습니다. U+ 고객만 할인 혜택을 받을 수 있습니다.",
                    null
            );
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static class SubscriptionResponse {

        private final String maskedPhoneNumber; // ex) 010-****-5678
        private final String plan;              // ex) "FIVE_G_PREMIUM"
        private final String planDisplayName;   // ex) "5G 프리미엄"
        private final int monthlyFee;
        private final boolean active;

        public static SubscriptionResponse from(UPlusSubscription s) {
            return new SubscriptionResponse(
                    maskPhone(s.getPhoneNumber()),
                    s.getPlan().name(),
                    s.getPlan().getDisplayName(),
                    s.getPlan().getMonthlyFee(),
                    s.isActive()
            );
        }

        private static String maskPhone(String phone) {
            if (phone == null || phone.length() < 11) return phone;
            return phone.substring(0, 3) + "-****-" + phone.substring(7);
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static class DiscountHistoryResponse {

        private final String billingYearMonth;
        private final String planDisplayName;
        private final int discountAmount;

        public static DiscountHistoryResponse from(UPlusBillDiscount d) {
            return new DiscountHistoryResponse(
                    d.getBillingYearMonth(),
                    d.getPlan().getDisplayName(),
                    d.getDiscountAmount()
            );
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static class PlanInfo {

        private final String code;
        private final String displayName;
        private final int monthlyFee;
    }
}