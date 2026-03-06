package com.ott.batch.monthly;

import com.ott.batch.monthly.support.BatchDateRange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;

/**
 * 매월 1일 00:00:00 KST에 월별 통계 배치를 실행한다.
 *
 * 실행 시 전월(1일 00:00:00 ~ 말일 23:59:59 KST) 범위를 Job Parameters로 전달.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyStatsJobLauncher {

    private final JobLauncher jobLauncher;

    @Qualifier("monthlyStatsJob")
    private final Job monthlyStatsJob;

    /**
     * 매월 1일 00:00:00 KST 실행
     *
     * runAt 파라미터를 포함하여 Spring Batch Job의 동일 파라미터 중복 실행 제한을 우회한다.
     * (실패 후 재실행 시 runAt만 바꿔 수동으로 재기동 가능)
     */
    @Scheduled(cron = "0 0 0 1 * *", zone = "Asia/Seoul")
    public void run() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        YearMonth previousMonth = BatchDateRange.previousMonth(today);

        OffsetDateTime rangeFrom = BatchDateRange.rangeFrom(previousMonth);
        OffsetDateTime rangeTo   = BatchDateRange.rangeTo(previousMonth);
        String yearMonth         = BatchDateRange.formatYearMonth(previousMonth);
        String runAt             = OffsetDateTime.now().toString();

        log.info("[MonthlyStatsJob] 배치 시작 - 집계 대상: {}, 범위: {} ~ {}", yearMonth, rangeFrom, rangeTo);

        try {
            JobParameters params = new JobParametersBuilder()
                    .addString("yearMonth",  yearMonth)
                    .addString("rangeFrom",  rangeFrom.toString())
                    .addString("rangeTo",    rangeTo.toString())
                    .addString("runAt",      runAt)   // 재실행 구분용
                    .toJobParameters();

            jobLauncher.run(monthlyStatsJob, params);
            log.info("[MonthlyStatsJob] 배치 완료 - {}", yearMonth);

        } catch (Exception e) {
            log.error("[MonthlyStatsJob] 배치 실행 중 에러 발생: {}", e.getMessage(), e);
        }
    }
}