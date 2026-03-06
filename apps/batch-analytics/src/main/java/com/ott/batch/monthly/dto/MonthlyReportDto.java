package com.ott.batch.monthly.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 월간 리포트 DTO (배치 내부용)
 */
@Getter
@Builder
public class MonthlyReportDto {

    private Long userId;
    private String reportYearMonth;

    // 기본 통계
    private Long totalWatchSeconds;
    private Integer totalWatchCount;
    private Integer completedCount;
    private BigDecimal completionRate;  // BigDecimal로 변경

    // 시간대별 통계
    private Integer dawnCount;
    private Integer morningCount;
    private Integer afternoonCount;
    private Integer eveningCount;
    private Integer nightCount;
    private String peakTimeSlot;

    // 부가 정보
    private Integer longestSessionSeconds;
    private String mostWatchedTagName;
    private Integer diversityScore;
}