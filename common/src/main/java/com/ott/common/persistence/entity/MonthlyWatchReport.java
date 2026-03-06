package com.ott.common.persistence.entity;

import com.ott.common.persistence.base.BaseEntity;
import com.ott.common.util.IdGenerator;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * 월별 개인 시청 리포트
 *
 * 매월 1일 00:00 배치가 전월(1일~말일) 시청 기록을 집계하여 저장한다.
 *
 * report_year_month = "2025-02" → 2025년 2월 전체 통계
 */
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(
        name = "monthly_watch_report",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_monthly_watch_report_user_month",
                        columnNames = {"user_id", "report_year_month"}
                )
        }
)
public class MonthlyWatchReport extends BaseEntity {

    @Id
    @Column(name = "monthly_watch_report_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 집계 대상 연월. 예: 3월 1일 배치 → "2025-02" */
    @Column(name = "report_year_month", nullable = false, length = 7)
    private String reportYearMonth;

    // ── 기본 지표 ──────────────────────────────────────────

    @Builder.Default
    @Column(name = "total_watch_seconds", nullable = false)
    private long totalWatchSeconds = 0L;

    @Builder.Default
    @Column(name = "total_watch_count", nullable = false)
    private int totalWatchCount = 0;

    @Builder.Default
    @Column(name = "completed_count", nullable = false)
    private int completedCount = 0;

    /** 완주율 (%). 소수점 2자리 */
    @Builder.Default
    @Column(name = "completion_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal completionRate = BigDecimal.ZERO;

    // ── 시간대별 시청 횟수 ────────────────────────────────

    /** 새벽 00~05시 */
    @Builder.Default
    @Column(name = "dawn_count", nullable = false)
    private int dawnCount = 0;

    /** 아침 06~11시 */
    @Builder.Default
    @Column(name = "morning_count", nullable = false)
    private int morningCount = 0;

    /** 오후 12~17시 */
    @Builder.Default
    @Column(name = "afternoon_count", nullable = false)
    private int afternoonCount = 0;

    /** 저녁 18~21시 */
    @Builder.Default
    @Column(name = "evening_count", nullable = false)
    private int eveningCount = 0;

    /** 야간 22~23시 */
    @Builder.Default
    @Column(name = "night_count", nullable = false)
    private int nightCount = 0;

    /**
     * 가장 많이 시청한 시간대
     * DAWN / MORNING / AFTERNOON / EVENING / NIGHT / NONE
     */
    @Builder.Default
    @Column(name = "peak_time_slot", nullable = false, length = 20)
    private String peakTimeSlot = "NONE";

    // ── 기록 ─────────────────────────────────────────────

    /** 한 영상에 대해 가장 오래 시청한 시간(초). lastPosition 최댓값 */
    @Builder.Default
    @Column(name = "longest_session_seconds", nullable = false)
    private int longestSessionSeconds = 0;

    /** 이달의 최애 태그명 */
    @Column(name = "most_watched_tag_name", length = 100)
    private String mostWatchedTagName;

    /**
     * 시청 다양성 점수 (0~100)
     *
     * 계산식: min(100, 고유 부모태그 수 × 20)
     * 5가지 이상 다른 장르를 골고루 시청하면 100점
     */
    @Builder.Default
    @Column(name = "diversity_score", nullable = false)
    private int diversityScore = 0;

    @PrePersist
    private void prePersist() {
        if (id == null) id = IdGenerator.generate();
    }
}