package com.ott.common.persistence.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * U+ 요금제 종류
 *
 * 포인트 할인 정책:
 * - 포인트 1점 = 1원
 * - 보유 포인트 전액 차감 (단, 월 기본료 상한)
 * - 매월 1일 스케줄러가 자동 처리
 */
@Getter
@RequiredArgsConstructor
public enum UPlusSubscriptionPlan {

    FIVE_G_STANDARD ("5G 스탠다드",  55_000),
    FIVE_G_PREMIUM  ("5G 프리미엄",  69_000),
    FIVE_G_SIGNATURE("5G 시그니처",  89_000),
    LTE_SLIM        ("LTE 슬림",     33_000),
    LTE_STANDARD    ("LTE 스탠다드", 44_000),
    LTE_PREMIUM     ("LTE 프리미엄", 55_000);

    private final String displayName;

    /** 월 기본료 (원) — 포인트 할인 상한선 */
    private final int monthlyFee;
}