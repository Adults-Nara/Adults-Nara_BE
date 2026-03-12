package com.ott.batch.monthly.step2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.YearMonth;

import com.ott.batch.monthly.support.BatchDateRange;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyReportReader {

    private final DataSource dataSource;

    public JdbcCursorItemReader<Long> reader(YearMonth ym) throws Exception {
        JdbcCursorItemReader<Long> reader = new JdbcCursorItemReaderBuilder<Long>()
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
        
        reader.afterPropertiesSet();  // CRITICAL: 추가!
        return reader;
    }
}
