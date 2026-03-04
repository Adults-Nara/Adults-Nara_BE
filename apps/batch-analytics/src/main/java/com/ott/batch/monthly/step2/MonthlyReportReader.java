package com.ott.batch.monthly.step2;

import com.ott.batch.monthly.dto.UserWatchDetailRaw;
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

/**
 * Step2 Reader: 시간대·완주 여부·태그 정보를 포함한 세부 시청 기록.
 *
 * 시간대는 KST 기준으로 추출 (AT TIME ZONE 'Asia/Seoul').
 * 한 시청 기록에 태그가 여럿일 수 있으므로 JOIN 후 중복 user_id가 발생함 →
 * Processor에서 userId 단위로 집계 후 단일 DTO로 변환.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MonthlyReportReader {

    private final DataSource dataSource;

    private static final String SQL = """
            SELECT
                wh.user_id,
                wh.last_position,
                wh.completed,
                CAST(EXTRACT(HOUR FROM wh.updated_at AT TIME ZONE 'Asia/Seoul') AS INTEGER) AS watch_hour,
                t.tag_name,
                tp.tag_name AS parent_tag_name
            FROM watch_history wh
            JOIN video_tag vt ON wh.video_metadata_id = vt.video_metadata_id
            JOIN tag t        ON vt.tag_id = t.tag_id
            LEFT JOIN tag tp  ON t.parent_id = tp.tag_id
            WHERE wh.updated_at >= ?
              AND wh.updated_at <= ?
              AND wh.deleted = false
            ORDER BY wh.user_id, wh.updated_at
            """;

    @Bean("monthlyReportItemReader")
    @StepScope
    public JdbcCursorItemReader<UserWatchDetailRaw> monthlyReportItemReader(
            @Value("#{jobParameters['rangeFrom']}") String rangeFromStr,
            @Value("#{jobParameters['rangeTo']}")   String rangeToStr
    ) {
        OffsetDateTime rangeFrom = OffsetDateTime.parse(rangeFromStr);
        OffsetDateTime rangeTo   = OffsetDateTime.parse(rangeToStr);

        log.info("[MonthlyReportReader] 집계 범위: {} ~ {}", rangeFrom, rangeTo);

        return new JdbcCursorItemReaderBuilder<UserWatchDetailRaw>()
                .name("monthlyReportItemReader")
                .dataSource(dataSource)
                .sql(SQL)
                .preparedStatementSetter(ps -> {
                    ps.setObject(1, rangeFrom);
                    ps.setObject(2, rangeTo);
                })
                .rowMapper((rs, rowNum) -> new UserWatchDetailRaw(
                        rs.getLong("user_id"),
                        rs.getInt("last_position"),
                        rs.getBoolean("completed"),
                        rs.getInt("watch_hour"),
                        rs.getString("tag_name"),
                        rs.getString("parent_tag_name")
                ))
                .fetchSize(500)
                .build();
    }
}