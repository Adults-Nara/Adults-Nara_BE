package com.ott.batch.monthly.step2;

import com.ott.batch.monthly.dto.MonthlyReportDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * MonthlyReport 데이터를 DB에 Batch Upsert하는 Writer
 * - 버퍼 없이 청크를 즉시 처리하여 메모리 효율성 향상
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyReportWriter implements ItemWriter<MonthlyReportDto> {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void write(Chunk<? extends MonthlyReportDto> chunk) {
        List<? extends MonthlyReportDto> items = chunk.getItems();

        if (items.isEmpty()) {
            return;
        }

        log.debug("[MonthlyReportWriter] {}건 처리 시작", items.size());

        String sql = """
            INSERT INTO monthly_watch_report (
                monthly_watch_report_id, user_id, report_year_month,
                total_watch_seconds, total_watch_count, completed_count, completion_rate,
                dawn_count, morning_count, afternoon_count, evening_count, night_count, peak_time_slot,
                longest_session_seconds, most_watched_tag_name, diversity_score,
                created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
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
        """;

        // 타입 안전성을 위해 List로 변환
        List<MonthlyReportDto> dtoList = new ArrayList<>(items);

        jdbcTemplate.batchUpdate(sql, dtoList, dtoList.size(),
                (PreparedStatement ps, MonthlyReportDto dto) -> {
                    ps.setLong(1, dto.getId());
                    ps.setLong(2, dto.getUserId());
                    ps.setString(3, dto.getReportYearMonth());
                    ps.setLong(4, dto.getTotalWatchSeconds() != null ? dto.getTotalWatchSeconds() : 0L);
                    ps.setInt(5, dto.getTotalWatchCount() != null ? dto.getTotalWatchCount() : 0);
                    ps.setInt(6, dto.getCompletedCount() != null ? dto.getCompletedCount() : 0);
                    ps.setBigDecimal(7, dto.getCompletionRate() != null ? dto.getCompletionRate() : java.math.BigDecimal.ZERO);
                    ps.setInt(8, dto.getDawnCount() != null ? dto.getDawnCount() : 0);
                    ps.setInt(9, dto.getMorningCount() != null ? dto.getMorningCount() : 0);
                    ps.setInt(10, dto.getAfternoonCount() != null ? dto.getAfternoonCount() : 0);
                    ps.setInt(11, dto.getEveningCount() != null ? dto.getEveningCount() : 0);
                    ps.setInt(12, dto.getNightCount() != null ? dto.getNightCount() : 0);
                    ps.setString(13, dto.getPeakTimeSlot());
                    ps.setInt(14, dto.getLongestSessionSeconds() != null ? dto.getLongestSessionSeconds() : 0);
                    ps.setString(15, dto.getMostWatchedTagName());
                    ps.setInt(16, dto.getDiversityScore() != null ? dto.getDiversityScore() : 0);
                }
        );

        log.info("[MonthlyReportWriter] {}건 batch upsert 완료", items.size());
    }
}