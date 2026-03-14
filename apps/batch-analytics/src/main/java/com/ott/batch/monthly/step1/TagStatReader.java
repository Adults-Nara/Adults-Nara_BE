package com.ott.batch.monthly.step1;

import com.ott.batch.monthly.dto.TagStatDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class TagStatReader {

    private final DataSource dataSource;

    public JdbcCursorItemReader<TagStatDto> reader(OffsetDateTime rangeFrom, OffsetDateTime rangeTo) {
        log.debug("[TagStatReader] SQL 준비 완료. 기간: {} ~ {}", rangeFrom, rangeTo);

        JdbcCursorItemReader<TagStatDto> reader = new JdbcCursorItemReaderBuilder<TagStatDto>()
                .name("tagStatItemReader")
                .dataSource(dataSource)
                .sql("""
                    SELECT
                        wh.user_id,
                        t.tag_id,
                        SUM(wh.last_position) AS total_view_time,
                        COUNT(wh.watch_history_id) AS view_count,
                        SUM(CASE WHEN wh.completed = true THEN 1 ELSE 0 END) AS completed_count
                    FROM watch_history wh
                    JOIN video_tag vt ON wh.video_metadata_id = vt.video_metadata_id
                    JOIN tag t ON vt.tag_id = t.tag_id
                    WHERE wh.created_at >= ?
                      AND wh.created_at < ?
                      AND wh.deleted = false
                    GROUP BY wh.user_id, t.tag_id
                    ORDER BY wh.user_id, t.tag_id
                    """)
                .preparedStatementSetter((ps) -> {
                    ps.setObject(1, rangeFrom);
                    ps.setObject(2, rangeTo);
                })
                .rowMapper(new TagStatRowMapper())
                .build();

        // CRITICAL: Reader 초기화
        try {
            reader.afterPropertiesSet();
        } catch (Exception e) {
            throw new RuntimeException("TagStatReader 초기화 실패", e);
        }

        return reader;
    }

    private static class TagStatRowMapper implements RowMapper<TagStatDto> {
        @Override
        public TagStatDto mapRow(ResultSet rs, int rowNum) throws SQLException {
            return TagStatDto.builder()
                    .userId(rs.getLong("user_id"))
                    .tagId(rs.getLong("tag_id"))
                    .totalViewTime(rs.getLong("total_view_time"))
                    .viewCount(rs.getInt("view_count"))
                    .completedCount(rs.getInt("completed_count"))
                    .build();
        }
    }
}