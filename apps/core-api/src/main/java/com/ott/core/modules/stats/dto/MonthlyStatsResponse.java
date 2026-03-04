package com.ott.core.modules.stats.dto;

import com.ott.common.persistence.entity.MonthlyWatchReport;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 마이페이지 월별 시청 통계 응답
 */
@Getter
@Builder
public class MonthlyStatsResponse {

    private String reportYearMonth;   // "2025-02"

    // ── 기본 지표 ──
    private long totalWatchSeconds;
    private int totalWatchCount;
    private int completedCount;
    private BigDecimal completionRate;

    // ── 시간대 분포 ──
    private TimeSlotStats timeSlotStats;
    private String peakTimeSlot;
    private String peakTimeSlotLabel;  // 한국어 레이블

    // ── 기록 ──
    private int longestSessionSeconds;
    private String longestSessionFormatted;  // "1시간 23분"

    // ── 태그 ──
    private String mostWatchedTagName;
    private List<TagStatSummary> tagStats;   // 태그별 세부 통계

    // ── 다양성 ──
    private int diversityScore;
    private String diversityLabel;  // "다양한 장르를 즐기시는군요!" 등

    @Getter
    @Builder
    public static class TimeSlotStats {
        private int dawnCount;
        private int morningCount;
        private int afternoonCount;
        private int eveningCount;
        private int nightCount;
    }

    @Getter
    @Builder
    public static class TagStatSummary {
        private String tagName;
        private long totalViewSeconds;
        private int viewCount;
    }

    /** MonthlyWatchReport 엔티티 → 응답 DTO 변환 (tagStats는 별도로 세팅) */
    public static MonthlyStatsResponse from(MonthlyWatchReport report, List<TagStatSummary> tagStats) {
        return MonthlyStatsResponse.builder()
                .reportYearMonth(report.getReportYearMonth())
                .totalWatchSeconds(report.getTotalWatchSeconds())
                .totalWatchCount(report.getTotalWatchCount())
                .completedCount(report.getCompletedCount())
                .completionRate(report.getCompletionRate())
                .timeSlotStats(TimeSlotStats.builder()
                        .dawnCount(report.getDawnCount())
                        .morningCount(report.getMorningCount())
                        .afternoonCount(report.getAfternoonCount())
                        .eveningCount(report.getEveningCount())
                        .nightCount(report.getNightCount())
                        .build())
                .peakTimeSlot(report.getPeakTimeSlot())
                .peakTimeSlotLabel(translatePeakSlot(report.getPeakTimeSlot()))
                .longestSessionSeconds(report.getLongestSessionSeconds())
                .longestSessionFormatted(formatSeconds(report.getLongestSessionSeconds()))
                .mostWatchedTagName(report.getMostWatchedTagName())
                .tagStats(tagStats)
                .diversityScore(report.getDiversityScore())
                .diversityLabel(buildDiversityLabel(report.getDiversityScore()))
                .build();
    }

    private static String translatePeakSlot(String slot) {
        return switch (slot) {
            case "DAWN"      -> "새벽형 (00~05시)";
            case "MORNING"   -> "아침형 (06~11시)";
            case "AFTERNOON" -> "오후형 (12~17시)";
            case "EVENING"   -> "저녁형 (18~21시)";
            case "NIGHT"     -> "야행성 (22~23시)";
            default          -> "-";
        };
    }

    private static String formatSeconds(int seconds) {
        if (seconds <= 0) return "0분";
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        if (hours > 0) return hours + "시간 " + minutes + "분";
        return minutes + "분";
    }

    private static String buildDiversityLabel(int score) {
        if (score >= 100) return "다양한 장르를 골고루 즐기시는군요! 🌟";
        if (score >= 80)  return "폭넓은 관심사를 갖고 계시네요! 👍";
        if (score >= 60)  return "여러 장르를 탐색 중이시군요!";
        if (score >= 40)  return "좋아하는 장르가 생기고 있어요.";
        if (score >= 20)  return "한 장르를 깊게 파고드시는 편이네요.";
        return "아직 탐색 중이에요. 다양한 콘텐츠를 즐겨보세요!";
    }
}