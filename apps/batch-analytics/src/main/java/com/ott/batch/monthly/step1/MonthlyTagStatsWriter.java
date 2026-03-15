package com.ott.batch.monthly.step1;

import com.ott.batch.monthly.dto.MonthlyReportDto;
import com.ott.common.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyTagStatsWriter implements ItemWriter<MonthlyReportDto> {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void write(Chunk<? extends MonthlyReportDto> chunk) {
        List<? extends MonthlyReportDto> items = chunk.getItems();
        
        if (items.isEmpty()) {
            return;
        }

        log.debug("[MonthlyTagStatsWriter] Batch Upsert 시작: {} 건", items.size());

        String sql = """
            INSERT INTO monthly_watch_report (
                monthly_watch_report_id, user_id, stats_year, stats_month,
                tag_id, tag_name, total_watch_seconds, watch_count
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (user_id, stats_year, stats_month, tag_id)
            DO UPDATE SET
                total_watch_seconds = EXCLUDED.total_watch_seconds,
                watch_count = EXCLUDED.watch_count,
                updated_at = NOW()
        """;

        jdbcTemplate.batchUpdate(
                sql,
                items,
                items.size(),
                (ps, dto) -> {
                    ps.setLong(1, IdGenerator.generate());  // ID 생성!
                    ps.setLong(2, dto.getUserId());
                    ps.setInt(3, dto.getStatsYear());
                    ps.setInt(4, dto.getStatsMonth());
                    ps.setLong(5, dto.getTagId());
                    ps.setString(6, dto.getTagName());
                    ps.setLong(7, dto.getTotalWatchSeconds());
                    ps.setInt(8, dto.getWatchCount());
                }
        );

        log.debug("[MonthlyTagStatsWriter] Batch Upsert 완료: {} 건", items.size());
    }
}
