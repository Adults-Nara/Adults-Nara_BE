package com.ott.batch.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.YearMonth;

/**
 * 월간 통계 배치 스케줄러
 * 매월 1일 새벽 2시에 전월 통계 생성
 */
@Slf4j
@Component
public class MonthlyStatsBatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job monthlyStatsJob;

    public MonthlyStatsBatchScheduler(JobLauncher jobLauncher, Job monthlyStatsJob) {
        this.jobLauncher = jobLauncher;
        this.monthlyStatsJob = monthlyStatsJob;
    }

    /**
     * 매월 1일 새벽 2시에 전월 통계 생성
     */
    @Scheduled(cron = "0 0 2 1 * ?")
    public void runMonthlyStatsBatch() {
        log.info("=== 월간 통계 배치 스케줄러 시작 ===");

        try {
            YearMonth lastMonth = YearMonth.now().minusMonths(1);
            String yearMonth = lastMonth.toString();

            OffsetDateTime rangeFrom = lastMonth.atDay(1)
                    .atStartOfDay()
                    .atOffset(OffsetDateTime.now().getOffset());

            OffsetDateTime rangeTo = lastMonth.atEndOfMonth()
                    .atTime(23, 59, 59)
                    .atOffset(OffsetDateTime.now().getOffset());

            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("yearMonth", yearMonth)
                    .addString("rangeFrom", rangeFrom.toString())
                    .addString("rangeTo", rangeTo.toString())
                    .addString("runAt", OffsetDateTime.now().toString())
                    .toJobParameters();

            log.info("배치 실행: yearMonth={}, rangeFrom={}, rangeTo={}",
                    yearMonth, rangeFrom, rangeTo);

            jobLauncher.run(monthlyStatsJob, jobParameters);

            log.info("=== 월간 통계 배치 스케줄러 완료 ===");

        } catch (Exception e) {
            log.error("월간 통계 배치 실행 실패", e);
        }
    }

    /**
     * 수동 실행용 메서드
     */
    public void runManually(String yearMonth) {
        log.info("=== 월간 통계 배치 수동 실행: {} ===", yearMonth);

        try {
            YearMonth ym = YearMonth.parse(yearMonth);

            OffsetDateTime rangeFrom = ym.atDay(1)
                    .atStartOfDay()
                    .atOffset(OffsetDateTime.now().getOffset());

            OffsetDateTime rangeTo = ym.atEndOfMonth()
                    .atTime(23, 59, 59)
                    .atOffset(OffsetDateTime.now().getOffset());

            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("yearMonth", yearMonth)
                    .addString("rangeFrom", rangeFrom.toString())
                    .addString("rangeTo", rangeTo.toString())
                    .addString("runAt", OffsetDateTime.now().toString())
                    .toJobParameters();

            jobLauncher.run(monthlyStatsJob, jobParameters);

            log.info("=== 월간 통계 배치 수동 실행 완료 ===");

        } catch (Exception e) {
            log.error("월간 통계 배치 수동 실행 실패: yearMonth={}", yearMonth, e);
            throw new RuntimeException("배치 실행 실패", e);
        }
    }
}