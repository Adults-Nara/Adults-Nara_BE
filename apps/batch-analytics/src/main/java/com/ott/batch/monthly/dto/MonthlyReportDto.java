package com.ott.batch.monthly.dto;

import com.ott.common.persistence.entity.MonthlyWatchReport;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyReportDto {
    private Long id;
    private Long userId;
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

    // toEntity 메서드 추가
    public MonthlyWatchReport toEntity() {
        return MonthlyWatchReport.builder()
                .id(this.id)
                .userId(this.userId)
                .reportYearMonth(this.reportYearMonth)
                .totalWatchSeconds(this.totalWatchSeconds)
                .totalWatchCount(this.totalWatchCount)
                .completedCount(this.completedCount)
                .completionRate(this.completionRate)
                .dawnCount(this.dawnCount)
                .morningCount(this.morningCount)
                .afternoonCount(this.afternoonCount)
                .eveningCount(this.eveningCount)
                .nightCount(this.nightCount)
                .peakTimeSlot(this.peakTimeSlot)
                .longestSessionSeconds(this.longestSessionSeconds)
                .mostWatchedTagName(this.mostWatchedTagName)
                .diversityScore(this.diversityScore)
                .build();
    }
}