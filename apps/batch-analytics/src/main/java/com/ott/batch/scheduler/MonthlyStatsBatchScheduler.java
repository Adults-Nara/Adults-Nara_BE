package com.ott.batch.scheduler;

import com.ott.batch.monthly.support.BatchDateRange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.YearMonth;

/**
 * 월간 통계 배치 스케줄러
 * - 매월 1일 02:00에 전월 통계 집계
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyStatsBatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job monthlyStatsJob;

    /**
     * 매월 1일 02:00 KST 자동 실행
     */
    @Scheduled(cron = "0 0 2 1 * ?", zone = "Asia/Seoul")
    public void runMonthlyStatsBatch() {
        YearMonth lastMonth = YearMonth.now().minusMonths(1);

        try {
            OffsetDateTime rangeFrom = BatchDateRange.rangeFrom(lastMonth);
            OffsetDateTime rangeTo = BatchDateRange.rangeTo(lastMonth);

            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("yearMonth", lastMonth.toString())
                    .addString("rangeFrom", rangeFrom.toString())
                    .addString("rangeTo", rangeTo.toString())
                    .addString("runAt", OffsetDateTime.now().toString())
                    .toJobParameters();

            JobExecution execution = jobLauncher.run(monthlyStatsJob, jobParameters);

            log.info("[스케줄러] 월간 통계 배치 완료 - yearMonth: {}, status: {}",
                    lastMonth, execution.getStatus());

        } catch (JobExecutionAlreadyRunningException | JobRestartException |
                 JobInstanceAlreadyCompleteException | JobParametersInvalidException e) {
            log.error("[스케줄러] 월간 통계 배치 실행 실패 - yearMonth: {}", lastMonth, e);
        }
    }

    /**
     * 수동 실행용 메서드
     */
    public void runManually(String yearMonth) throws JobInstanceAlreadyCompleteException,
            JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {

        YearMonth ym = YearMonth.parse(yearMonth);

        OffsetDateTime rangeFrom = BatchDateRange.rangeFrom(ym);
        OffsetDateTime rangeTo = BatchDateRange.rangeTo(ym);

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("yearMonth", yearMonth)
                .addString("rangeFrom", rangeFrom.toString())
                .addString("rangeTo", rangeTo.toString())
                .addString("runAt", OffsetDateTime.now().toString())
                .toJobParameters();

        JobExecution execution = jobLauncher.run(monthlyStatsJob, jobParameters);

        log.info("[수동 실행] 월간 통계 배치 완료 - yearMonth: {}, status: {}",
                yearMonth, execution.getStatus());
    }
}