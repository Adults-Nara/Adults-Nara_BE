package com.ott.batch.monthly.step2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Step 2: 월간 리포트 Reader
 * 태그 통계가 있는 모든 사용자 ID를 조회
 */
@Slf4j
@Component
public class MonthlyReportReader {

    private final DataSource dataSource;

    public MonthlyReportReader(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public JdbcCursorItemReader<Long> reader(String yearMonth) {
        // yearMonth를 LocalDate로 변환
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        LocalDate firstDayOfMonth = LocalDate.parse(yearMonth + "-01");
        LocalDate startDate = firstDayOfMonth;
        LocalDate endDate = firstDayOfMonth.withDayOfMonth(firstDayOfMonth.lengthOfMonth());

        String sql = """
            SELECT DISTINCT user_id
            FROM tag_stats
            WHERE stats_date >= ?
              AND stats_date <= ?
            ORDER BY user_id
        """;

        log.debug("[MonthlyReportReader] SQL 준비 완료. 기간: {} ~ {}", startDate, endDate);

        return new JdbcCursorItemReaderBuilder<Long>()
                .name("monthlyReportReader")
                .dataSource(dataSource)
                .sql(sql)
                .preparedStatementSetter(ps -> {
                    ps.setObject(1, startDate);
                    ps.setObject(2, endDate);
                })
                .rowMapper((ResultSet rs, int rowNum) -> rs.getLong("user_id"))
                .build();
    }
}