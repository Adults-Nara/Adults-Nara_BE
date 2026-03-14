package com.ott.batch.monthly.step1;

import com.ott.batch.monthly.dto.TagStatDto;
import com.ott.batch.monthly.support.BatchDateRange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.OffsetDateTime;
import java.time.YearMonth;

/**
 * Step 1: 월간 태그별 시청 통계 Reader
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyTagStatsReader {

    private final DataSource dataSource;

    public JdbcCursorItemReader<TagStatDto> reader(YearMonth yearMonth) {
        OffsetDateTime rangeFrom = BatchDateRange.rangeFrom(yearMonth);
        OffsetDateTime rangeTo = BatchDateRange.rangeTo(yearMonth);

        log.debug("[MonthlyTagStatsReader] 조회 범위: {} ~ {}", rangeFrom, rangeTo);

        JdbcCursorItemReader<TagStatDto> reader = new JdbcCursorItemReaderBuilder<TagStatDto>()
                .name("monthlyTagStatsReader")
                .dataSource(dataSource)
                .sql("""
                    SELECT 
                        wh.user_id,
                        vt.tag_id,
                        t.tag_name,
                        SUM(wh.total_watch_seconds) as total_watch_seconds,
                        COUNT(wh.watch_history_id) as watch_count
                    FROM watch_history wh
                    INNER JOIN video_metadata vm ON wh.video_metadata_id = vm.video_metadata_id
                    INNER JOIN video_tag vt ON vm.video_metadata_id = vt.video_metadata_id
                    INNER JOIN tag t ON vt.tag_id = t.tag_id
                    WHERE wh.created_at >= ?::timestamptz
                      AND wh.created_at < ?::timestamptz
                      AND wh.deleted = false
                      AND vm.deleted = false
                    GROUP BY wh.user_id, vt.tag_id, t.tag_name
                    ORDER BY wh.user_id, total_watch_seconds DESC
                """)
                .preparedStatementSetter(ps -> {
                    ps.setObject(1, rangeFrom);
                    ps.setObject(2, rangeTo);
                })
                .rowMapper((rs, rowNum) -> TagStatDto.builder()
                        .userId(rs.getLong("user_id"))
                        .tagId(rs.getLong("tag_id"))
                        .tagName(rs.getString("tag_name"))
                        .totalWatchSeconds(rs.getLong("total_watch_seconds"))
                        .watchCount(rs.getInt("watch_count"))
                        .build())
                .build();

        try {
            reader.afterPropertiesSet();
        } catch (Exception e) {
            throw new RuntimeException("MonthlyTagStatsReader 초기화 실패", e);
        }

        return reader;
    }
}
