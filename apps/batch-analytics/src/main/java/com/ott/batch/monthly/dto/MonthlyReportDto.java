package com.ott.batch.monthly.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Step 2: 월간 리포트 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyReportDto {
    private Long userId;
    private String reportYearMonth;
    private Long totalWatchSeconds;
    private Integer totalWatchCount;
    private Integer completedCount;
    private Double completionRate;        // getter 있음
    private Integer dawnCount;
    private Integer morningCount;
    private Integer afternoonCount;
    private Integer eveningCount;
    private Integer nightCount;
    private String peakTimeSlot;
    private Integer longestSessionSeconds;
    private String mostWatchedTagName;
    private Integer diversityScore;
}