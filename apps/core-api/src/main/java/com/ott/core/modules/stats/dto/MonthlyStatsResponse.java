package com.ott.core.modules.stats.dto;

import com.ott.common.persistence.entity.MonthlyWatchReport;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 월간 통계 응답 DTO
 */
@Getter
@Builder
public class MonthlyStatsResponse {

    private String reportYearMonth;

    // 기본 통계
    private Integer totalWatchCount;
    private Long totalWatchSeconds;
    private Integer completedCount;
    private BigDecimal completionRate;  // BigDecimal로 수정

    // 시간대별 통계
    private Integer dawnCount;      // 새벽 (0-5시)
    private Integer morningCount;   // 오전 (6-11시)
    private Integer afternoonCount; // 오후 (12-17시)
    private Integer eveningCount;   // 저녁 (18-21시)
    private Integer nightCount;     // 밤 (22-23시)
    private String peakTimeSlot;    // 주 시청 시간대

    // 부가 정보
    private Integer longestSessionSeconds;
    private String mostWatchedTagName;
    private Integer diversityScore;

    public static MonthlyStatsResponse from(MonthlyWatchReport report) {
        return MonthlyStatsResponse.builder()
                .reportYearMonth(report.getReportYearMonth())
                .totalWatchCount(report.getTotalWatchCount())
                .totalWatchSeconds(report.getTotalWatchSeconds())
                .completedCount(report.getCompletedCount())
                .completionRate(report.getCompletionRate())
                .dawnCount(report.getDawnCount())
                .morningCount(report.getMorningCount())
                .afternoonCount(report.getAfternoonCount())
                .eveningCount(report.getEveningCount())
                .nightCount(report.getNightCount())
                .peakTimeSlot(report.getPeakTimeSlot())
                .longestSessionSeconds(report.getLongestSessionSeconds())
                .mostWatchedTagName(report.getMostWatchedTagName())
                .diversityScore(report.getDiversityScore())
                .build();
    }
}