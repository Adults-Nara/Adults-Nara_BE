package com.ott.batch.monthly.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * monthly_watch_report 테이블에 저장될 집계 데이터.
 */
@Getter
@Builder
public class MonthlyReportDto {

    private Long userId;

    /** 집계 대상 연월 (YYYY-MM) */
    private String reportYearMonth;

    private long totalWatchSeconds;
    private int totalWatchCount;
    private int completedCount;
    private BigDecimal completionRate;

    private int dawnCount;
    private int morningCount;
    private int afternoonCount;
    private int eveningCount;
    private int nightCount;
    private String peakTimeSlot;

    private int longestSessionSeconds;
    private String mostWatchedTagName;
    private int diversityScore;
}