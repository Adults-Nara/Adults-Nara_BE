package com.ott.core.modules.uplus.dto;

import com.ott.common.persistence.entity.UPlusBillDiscount;
import com.ott.common.persistence.entity.UPlusSubscription;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

public class UPlusSubscriptionDto {

    // ===== Request =====

    @Getter
    @Setter
    @NoArgsConstructor
    public static class LinkRequest {

        @NotBlank(message = "전화번호는 필수입니다.")
        @Pattern(
                regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$",
                message = "올바르지 않은 전화번호 형식입니다."
        )
        private String phoneNumber;
    }

    // ===== Response =====

    @Getter
    @RequiredArgsConstructor
    public static class LinkResponse {
        private final String message;
        private final SubscriptionResponse subscription;

        public static LinkResponse success(UPlusSubscription s) {
            return new LinkResponse(
                    "U+ 가입 정보가 확인되었습니다.",
                    SubscriptionResponse.from(s)
            );
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static class SubscriptionResponse {

        private final String maskedPhoneNumber;
        private final String plan;
        private final String planDisplayName;
        private final int monthlyFee;
        private final boolean active;

        public static SubscriptionResponse from(UPlusSubscription s) {
            return new SubscriptionResponse(
                    maskPhoneNumber(s.getPhoneNumber()),
                    s.getPlan().name(),
                    s.getPlan().getDisplayName(),
                    s.getPlan().getMonthlyFee(),
                    s.isActive()
            );
        }

        private static String maskPhoneNumber(String phone) {
            if (phone == null || phone.length() < 10) return phone;
            if (phone.length() == 10) {
                return phone.substring(0, 3) + "-***-" + phone.substring(6);
            }
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
}