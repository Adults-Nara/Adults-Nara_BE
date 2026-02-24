package com.ott.common.persistence.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PointPolicy {
    AD_REWARD(5, "광고 시청 적립"),
    PURCHASE_RATE(3, "상품 구매 적립 비율(%)"),
    MIN_REDEEM_AMOUNT(3000, "최소 환급 포인트"),
    DAILY_AD_LIMIT(10, "일일 광고 적립 제한 횟수");

    private final int value;
    private final String description;
}