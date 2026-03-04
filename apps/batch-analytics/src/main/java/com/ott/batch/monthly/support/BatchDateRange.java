package com.ott.batch.monthly.support;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 배치 실행 시점 기준으로 "전월"의 시작/종료 시각을 계산한다.
 *
 * 예) 실행일 2025-03-01 → 전월 = 2025-02
 *     rangeFrom = 2025-02-01 00:00:00 KST
 *     rangeTo   = 2025-02-28 23:59:59 KST
 */
public final class BatchDateRange {

    public static final ZoneId KST = ZoneId.of("Asia/Seoul");
    public static final DateTimeFormatter YEAR_MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private BatchDateRange() {}

    /**
     * 실행 시점 기준 전월을 반환.
     * @param executionDate 배치 실행 날짜 (보통 오늘, 즉 매월 1일)
     */
    public static YearMonth previousMonth(LocalDate executionDate) {
        return YearMonth.from(executionDate.minusMonths(1));
    }

    /**
     * 전월 첫 날 00:00:00 KST → UTC OffsetDateTime
     */
    public static OffsetDateTime rangeFrom(YearMonth month) {
        return month.atDay(1)
                .atStartOfDay(KST)
                .toOffsetDateTime();
    }

    /**
     * 전월 마지막 날 23:59:59 KST → UTC OffsetDateTime
     */
    public static OffsetDateTime rangeTo(YearMonth month) {
        return month.atEndOfMonth()
                .atTime(23, 59, 59)
                .atZone(KST)
                .toOffsetDateTime();
    }

    /** "yyyy-MM" 문자열 반환 */
    public static String formatYearMonth(YearMonth month) {
        return month.format(YEAR_MONTH_FMT);
    }

    /** stats_date로 사용할 전월 1일 */
    public static LocalDate statsDate(YearMonth month) {
        return month.atDay(1);
    }
}