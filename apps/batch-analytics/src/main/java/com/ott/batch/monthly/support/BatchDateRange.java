package com.ott.batch.monthly.support;

import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;

/**
 * 배치 작업에서 사용하는 날짜 범위 유틸리티
 * - KST 기준으로 통일
 */
public class BatchDateRange {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /**
     * 월의 첫날 00:00:00 (KST)
     *
     * @param month 대상 월 (예: 2026-03)
     * @return 2026-03-01T00:00:00+09:00
     */
    public static OffsetDateTime rangeFrom(YearMonth month) {
        return month.atDay(1)
                .atStartOfDay(KST)
                .toOffsetDateTime();
    }

    /**
     * 다음 달의 첫날 00:00:00 (KST)
     * - SQL의 `created_at < rangeTo` 조건에서 월 마지막 데이터 누락 방지
     *
     * @param month 대상 월 (예: 2026-03)
     * @return 2026-04-01T00:00:00+09:00
     */
    public static OffsetDateTime rangeTo(YearMonth month) {
        return month.plusMonths(1)
                .atDay(1)
                .atStartOfDay(KST)
                .toOffsetDateTime();
    }
}