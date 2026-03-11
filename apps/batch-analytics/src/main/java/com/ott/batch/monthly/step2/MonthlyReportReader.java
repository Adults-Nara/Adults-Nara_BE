package com.ott.batch.monthly.step2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.YearMonth;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyReportReader {

    private final DataSource dataSource;

    public JdbcCursorItemReader<Long> reader(String yearMonth) {
        LocalDate firstDayOfMonth = YearMonth.parse(yearMonth).atDay(1);
        LocalDate lastDayOfMonth = YearMonth.parse(yearMonth).atEndOfMonth();

        log.debug("[MonthlyReportReader] SQL 준비 완료. 기간: {} ~ {}", firstDayOfMonth, lastDayOfMonth);

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
                    ps.setObject(1, firstDayOfMonth.atStartOfDay().toString() + "+09:00");
                    ps.setObject(2, lastDayOfMonth.plusDays(1).atStartOfDay().toString() + "+09:00");
                })
                .rowMapper((rs, rowNum) -> rs.getLong("user_id"))
                .build();
    }
}