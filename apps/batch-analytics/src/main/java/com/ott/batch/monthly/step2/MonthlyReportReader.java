package com.ott.batch.monthly.step2;

import com.ott.batch.monthly.support.BatchDateRange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.YearMonth;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyReportReader {

    private final DataSource dataSource;

    public JdbcCursorItemReader<Long> reader(String yearMonth) {
        YearMonth ym = YearMonth.parse(yearMonth);

        log.debug("[MonthlyReportReader] SQL 준비 완료. 기간: {}", yearMonth);

        return new JdbcCursorItemReaderBuilder<Long>()
                .name("monthlyReportItemReader")
                .dataSource(dataSource)
                .sql("""
                    SELECT DISTINCT wh.user_id
                    FROM watch_history wh
                    WHERE wh.created_at >= ?::timestamptz
                      AND wh.created_at < ?::timestamptz
                      AND wh.deleted = false
                    ORDER BY wh.user_id
                    """)
                .preparedStatementSetter(ps -> {
                    ps.setObject(1, BatchDateRange.rangeFrom(ym));
                    ps.setObject(2, BatchDateRange.rangeTo(ym));
                })
                .rowMapper((rs, rowNum) -> rs.getLong("user_id"))
                .build();
    }
}