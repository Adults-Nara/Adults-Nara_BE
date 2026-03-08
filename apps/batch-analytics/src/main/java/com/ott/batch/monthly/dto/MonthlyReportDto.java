package com.ott.batch.monthly.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class MonthlyReportDto {

    private Long id;  // ID 필드 추가
    private Long userId;
    private String reportYearMonth;
    private Long totalWatchSeconds;
    private Integer totalWatchCount;
    private Integer completedCount;
    private BigDecimal completionRate;
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