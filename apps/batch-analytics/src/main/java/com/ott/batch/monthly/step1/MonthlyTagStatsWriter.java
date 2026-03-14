package com.ott.batch.monthly.step1;

import com.ott.batch.monthly.dto.MonthlyReportDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Step 1: 월간 태그별 시청 통계 Writer (Batch Upsert)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyTagStatsWriter implements ItemWriter<MonthlyReportDto> {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void write(Chunk<? extends MonthlyReportDto> chunk) {
        List<MonthlyReportDto> items = new ArrayList<>(chunk.getItems());
        
        if (items.isEmpty()) {
            return;
        }

        String sql = """
            INSERT INTO monthly_watch_report (
                monthly_watch_report_id, user_id, stats_year, stats_month,
                tag_id, tag_name, total_watch_seconds, watch_count,
                created_at, updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
            ON CONFLICT (user_id, stats_year, stats_month, tag_id) DO UPDATE SET
                total_watch_seconds = EXCLUDED.total_watch_seconds,
                watch_count = EXCLUDED.watch_count,
                updated_at = NOW()
        """;

        jdbcTemplate.batchUpdate(
                sql,
                items,
                items.size(),
                (ps, dto) -> {
                    ps.setLong(1, dto.getId());
                    ps.setLong(2, dto.getUserId());
                    ps.setInt(3, dto.getStatsYear());
                    ps.setInt(4, dto.getStatsMonth());
                    ps.setLong(5, dto.getTagId());
                    ps.setString(6, dto.getTagName());
                    ps.setLong(7, dto.getTotalWatchSeconds());
                    ps.setInt(8, dto.getWatchCount());
                }
        );

        log.debug("[MonthlyTagStatsWriter] {}건 batch upsert 완료", items.size());
    }
}
