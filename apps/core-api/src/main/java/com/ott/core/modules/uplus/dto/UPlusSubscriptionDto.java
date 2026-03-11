package com.ott.core.modules.uplus.dto;

import com.ott.common.persistence.entity.UPlusBillDiscount;
import com.ott.common.persistence.entity.UPlusSubscription;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class UPlusSubscriptionDto {

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