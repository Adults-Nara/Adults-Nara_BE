package com.ott.batch.monthly.step1;

import com.ott.batch.monthly.dto.UserTagWatchRaw;
import com.ott.batch.monthly.support.BatchDateRange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.time.OffsetDateTime;
import java.time.YearMonth;

/**
 * Step1 Reader: watch_history + video_tag + tag 조인으로
 * 전월 시청 기록을 사용자별·태그별로 집계하여 읽어온다.
 *
 * JDBC Cursor 방식으로 대용량 처리에 안전하다.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class TagStatReader {

    private final DataSource dataSource;

    private static final String SQL = """
            SELECT
                wh.user_id,
                t.tag_id,
                t.tag_name,
                CAST(SUM(wh.last_position) AS INTEGER)  AS total_view_time,
                CAST(COUNT(*)              AS INTEGER)  AS view_count
            FROM watch_history wh
            JOIN video_tag vt ON wh.video_metadata_id = vt.video_metadata_id
            JOIN tag t        ON vt.tag_id = t.tag_id
            WHERE wh.updated_at >= ?
              AND wh.updated_at <= ?
              AND wh.deleted = false
            GROUP BY wh.user_id, t.tag_id, t.tag_name
            ORDER BY wh.user_id, t.tag_id
            """;

    @Bean("tagStatItemReader")
    @StepScope
    public JdbcCursorItemReader<UserTagWatchRaw> tagStatItemReader(
            @Value("#{jobParameters['rangeFrom']}") String rangeFromStr,
            @Value("#{jobParameters['rangeTo']}")   String rangeToStr
    ) {
        OffsetDateTime rangeFrom = OffsetDateTime.parse(rangeFromStr);
        OffsetDateTime rangeTo   = OffsetDateTime.parse(rangeToStr);

        log.info("[TagStatReader] 집계 범위: {} ~ {}", rangeFrom, rangeTo);

        return new JdbcCursorItemReaderBuilder<UserTagWatchRaw>()
                .name("tagStatItemReader")
                .dataSource(dataSource)
                .sql(SQL)
                .preparedStatementSetter(ps -> {
                    ps.setObject(1, rangeFrom);
                    ps.setObject(2, rangeTo);
                })
                .rowMapper((rs, rowNum) -> new UserTagWatchRaw(
                        rs.getLong("user_id"),
                        rs.getLong("tag_id"),
                        rs.getString("tag_name"),
                        rs.getInt("total_view_time"),
                        rs.getInt("view_count")
                ))
                .fetchSize(500)
                .build();
    }
}