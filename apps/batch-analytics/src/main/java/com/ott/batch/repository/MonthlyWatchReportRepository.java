package com.ott.batch.repository;

import com.ott.common.persistence.entity.MonthlyWatchReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

public interface MonthlyWatchReportRepository extends JpaRepository<MonthlyWatchReport, Long> {

    Optional<MonthlyWatchReport> findByUserIdAndReportYearMonth(Long userId, String reportYearMonth);

    /**
     * 월별 리포트 upsert
     * 재실행 안전성 보장: 동일 (user_id, report_year_month) 존재 시 덮어쓴다
     */
    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO monthly_watch_report (
                monthly_watch_report_id, user_id, report_year_month,
                total_watch_seconds, total_watch_count, completed_count, completion_rate,
                dawn_count, morning_count, afternoon_count, evening_count, night_count, peak_time_slot,
                longest_session_seconds, most_watched_tag_name, diversity_score,
                created_at, updated_at
            ) VALUES (
                :id, :userId, :reportYearMonth,
                :totalWatchSeconds, :totalWatchCount, :completedCount, :completionRate,
                :dawnCount, :morningCount, :afternoonCount, :eveningCount, :nightCount, :peakTimeSlot,
                :longestSessionSeconds, :mostWatchedTagName, :diversityScore,
                NOW(), NOW()
            )
            ON CONFLICT (user_id, report_year_month)
            DO UPDATE SET
                total_watch_seconds     = EXCLUDED.total_watch_seconds,
                total_watch_count       = EXCLUDED.total_watch_count,
                completed_count         = EXCLUDED.completed_count,
                completion_rate         = EXCLUDED.completion_rate,
                dawn_count              = EXCLUDED.dawn_count,
                morning_count           = EXCLUDED.morning_count,
                afternoon_count         = EXCLUDED.afternoon_count,
                evening_count           = EXCLUDED.evening_count,
                night_count             = EXCLUDED.night_count,
                peak_time_slot          = EXCLUDED.peak_time_slot,
                longest_session_seconds = EXCLUDED.longest_session_seconds,
                most_watched_tag_name   = EXCLUDED.most_watched_tag_name,
                diversity_score         = EXCLUDED.diversity_score,
                updated_at              = NOW()
            """, nativeQuery = true)
    void upsertMonthlyReport(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("reportYearMonth") String reportYearMonth,
            @Param("totalWatchSeconds") long totalWatchSeconds,
            @Param("totalWatchCount") int totalWatchCount,
            @Param("completedCount") int completedCount,
            @Param("completionRate") BigDecimal completionRate,
            @Param("dawnCount") int dawnCount,
            @Param("morningCount") int morningCount,
            @Param("afternoonCount") int afternoonCount,
            @Param("eveningCount") int eveningCount,
            @Param("nightCount") int nightCount,
            @Param("peakTimeSlot") String peakTimeSlot,
            @Param("longestSessionSeconds") int longestSessionSeconds,
            @Param("mostWatchedTagName") String mostWatchedTagName,
            @Param("diversityScore") int diversityScore
    );
}