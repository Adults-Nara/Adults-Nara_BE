package com.ott.batch.monthly.step1;

import com.ott.batch.monthly.dto.TagStatDto;  // 올바른 패키지
import com.ott.common.util.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.util.List;

/**
 * Step 1: 태그별 통계 Writer (Batch Update 방식)
 *
 * 🟢 IMPROVEMENT: N+1 쓰기 → Batch Update로 성능 개선
 */
@Slf4j
@Component
public class TagStatWriter implements ItemWriter<TagStatDto> {

    private final JdbcTemplate jdbcTemplate;

    public TagStatWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void write(Chunk<? extends TagStatDto> chunk) {
        List<? extends TagStatDto> items = chunk.getItems();

        if (items.isEmpty()) {
            return;
        }

        String sql = """
            INSERT INTO tag_stats (tag_stats_id, user_id, tag_id, stats_date, total_view_time, view_count, completed_count, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, NOW())
            ON CONFLICT (user_id, tag_id, stats_date) DO UPDATE SET
                total_view_time = EXCLUDED.total_view_time,
                view_count      = EXCLUDED.view_count,
                completed_count = EXCLUDED.completed_count,
                updated_at      = NOW()
        """;

        jdbcTemplate.batchUpdate(
                sql,
                items,
                items.size(),
                (ps, dto) -> {
                    ps.setLong(1, IdGenerator.generate());
                    ps.setLong(2, dto.getUserId());
                    ps.setLong(3, dto.getTagId());
                    ps.setObject(4, dto.getStatsDate());
                    ps.setLong(5, dto.getTotalViewTime());
                    ps.setInt(6, dto.getViewCount());
                    ps.setInt(7, dto.getCompletedCount());
                }
        );

        log.debug("[TagStatWriter] {}건 batch upsert 완료", items.size());
    }
}