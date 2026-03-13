package com.ott.core.modules.uplus.dto;

import com.ott.common.persistence.entity.UPlusBillDiscount;
import com.ott.common.persistence.entity.UPlusSubscription;
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
        private String phoneNumber;  // @Valid 제거 - 형식 오류도 false로 반환
    }

    // ===== Response =====

    @Getter
    @RequiredArgsConstructor
    public static class LinkResponse {
        private final boolean verified;
        private final String message;
        private final SubscriptionResponse subscription;

        public static LinkResponse success(UPlusSubscription s) {
            return new LinkResponse(
                    true,
                    "U+ 가입 정보가 확인되었습니다.",
                    SubscriptionResponse.from(s)
            );
        }

        public static LinkResponse notFound() {
            return new LinkResponse(
                    false,
                    "입력한 정보와 일치하는 가입 정보를 찾을 수 없습니다.",
                    null
            );
        }

        public static LinkResponse inactive() {
            return new LinkResponse(
                    false,
                    "해지된 U+ 가입 정보입니다.",
                    null
            );
        }

        public static LinkResponse wrongNumber() {
            return new LinkResponse(
                    false,
                    "잘못된 번호입니다.",
                    null
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