package com.ott.batch.monthly.step1;

import com.ott.batch.monthly.dto.TagStatDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Step 1: 태그별 일별 통계 Reader
 * 기간 내 각 태그별 시청 기록을 집계
 */
@Slf4j
@Component
public class TagStatReader {

    private final DataSource dataSource;

    public TagStatReader(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public JdbcCursorItemReader<TagStatDto> reader(OffsetDateTime rangeFrom, OffsetDateTime rangeTo) {
        // WatchHistory에는 watched_at 컬럼이 없으므로 created_at 사용
        String sql = """
            SELECT
                vt.tag_id,
                wh.user_id,
                DATE(wh.created_at) AS stats_date,
                SUM(wh.last_position) AS total_view_time,
                COUNT(wh.watch_history_id) AS view_count,
                SUM(CASE WHEN wh.completed THEN 1 ELSE 0 END) AS completed_count
            FROM watch_history wh
            INNER JOIN video_tag vt ON wh.video_metadata_id = vt.video_metadata_id
            WHERE wh.deleted = false
              AND wh.created_at >= ?
              AND wh.created_at < ?
            GROUP BY vt.tag_id, wh.user_id, DATE(wh.created_at)
            ORDER BY wh.user_id, DATE(wh.created_at), vt.tag_id
        """;

        log.debug("[TagStatReader] SQL 준비 완료. 기간: {} ~ {}", rangeFrom, rangeTo);

        return new JdbcCursorItemReaderBuilder<TagStatDto>()
                .name("tagStatReader")
                .dataSource(dataSource)
                .sql(sql)
                .preparedStatementSetter(ps -> {
                    ps.setObject(1, rangeFrom);
                    ps.setObject(2, rangeTo);
                })
                .rowMapper(new TagStatRowMapper())
                .build();
    }

    private static class TagStatRowMapper implements RowMapper<TagStatDto> {
        @Override
        public TagStatDto mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new TagStatDto(
                    rs.getLong("tag_id"),
                    rs.getLong("user_id"),
                    rs.getObject("stats_date", LocalDate.class),
                    rs.getLong("total_view_time"),
                    rs.getInt("view_count"),
                    rs.getInt("completed_count")
            );
        }
    }
}