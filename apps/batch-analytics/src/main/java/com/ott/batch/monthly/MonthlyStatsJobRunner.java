// apps/batch-analytics/src/main/java/com/ott/batch/monthly/MonthlyStatsJobRunner.java

package com.ott.batch.monthly;


import com.ott.batch.monthly.support.BatchDateRange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.YearMonth;

/**
 * 로컬 수동 실행용 Runner.
 * --yearMonth=2025-02 인자를 넘기면 해당 월 배치를 즉시 실행한다.
 *
 * 실행 방법:
 *   ./gradlew :apps:batch-analytics:bootRun --args='--yearMonth=2025-02'
 *
 * @Profile("local") 로 prod 환경에서는 절대 실행되지 않는다.
 */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class MonthlyStatsJobRunner implements ApplicationRunner {

    private final JobLauncher jobLauncher;

    @Qualifier("monthlyStatsJob")
    private final Job monthlyStatsJob;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!args.containsOption("yearMonth")) {
            log.info("[MonthlyStatsJobRunner] --yearMonth 인자 없음. 수동 실행 건너뜀.");
            return;
        }

        String yearMonth = args.getOptionValues("yearMonth").get(0);
        YearMonth ym     = YearMonth.parse(yearMonth);

        OffsetDateTime rangeFrom = BatchDateRange.rangeFrom(ym);
        OffsetDateTime rangeTo   = BatchDateRange.rangeTo(ym);

        log.info("[MonthlyStatsJobRunner] 수동 실행 시작 - yearMonth: {}", yearMonth);

        JobParameters params = new JobParametersBuilder()
                .addString("yearMonth", yearMonth)
                .addString("rangeFrom", rangeFrom.toString())
                .addString("rangeTo",   rangeTo.toString())
                .addString("runAt",     OffsetDateTime.now().toString())
                .toJobParameters();

        jobLauncher.run(monthlyStatsJob, params);

        log.info("[MonthlyStatsJobRunner] 수동 실행 완료 - yearMonth: {}", yearMonth);
    }
}