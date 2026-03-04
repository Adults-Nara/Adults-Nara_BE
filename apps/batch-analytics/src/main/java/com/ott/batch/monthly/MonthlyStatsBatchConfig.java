package com.ott.batch.monthly;

import com.ott.batch.monthly.dto.MonthlyReportDto;
import com.ott.batch.monthly.dto.TagStatDto;
import com.ott.batch.monthly.dto.UserTagWatchRaw;
import com.ott.batch.monthly.dto.UserWatchDetailRaw;
import com.ott.batch.monthly.step1.TagStatProcessor;
import com.ott.batch.monthly.step1.TagStatWriter;
import com.ott.batch.monthly.step2.MonthlyReportProcessor;
import com.ott.batch.monthly.step2.MonthlyReportWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 월별 시청 통계 배치 Job 설정.
 *
 * Job: monthlyStatsJob
 * ├── Step1: tagStatisticsStep   (태그별 시청 시간/횟수 → tag_stats)
 * └── Step2: monthlyReportStep   (개인 리포트 지표 → monthly_watch_report)
 *
 * Job Parameters:
 *   - yearMonth  : "yyyy-MM"  (집계 대상 전월, 예: "2025-02")
 *   - rangeFrom  : ISO OffsetDateTime 문자열 (전월 1일 00:00:00 KST)
 *   - rangeTo    : ISO OffsetDateTime 문자열 (전월 말일 23:59:59 KST)
 *   - runAt      : 실행 시각 (재실행 구분용 고유값, ISO OffsetDateTime)
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MonthlyStatsBatchConfig {

    private static final int CHUNK_SIZE = 100;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    // Step1
    private final JdbcCursorItemReader<UserTagWatchRaw> tagStatItemReader;
    private final TagStatProcessor tagStatItemProcessor;
    private final TagStatWriter tagStatItemWriter;

    // Step2
    private final JdbcCursorItemReader<UserWatchDetailRaw> monthlyReportItemReader;
    private final MonthlyReportProcessor monthlyReportItemProcessor;
    private final MonthlyReportWriter monthlyReportItemWriter;

    @Bean("monthlyStatsJob")
    public Job monthlyStatsJob() {
        return new JobBuilder("monthlyStatsJob", jobRepository)
                .start(tagStatisticsStep())
                .next(monthlyReportStep())
                .build();
    }

    /**
     * Step1: 태그별 시청 통계 집계 → tag_stats upsert
     */
    @Bean("tagStatisticsStep")
    public Step tagStatisticsStep() {
        return new StepBuilder("tagStatisticsStep", jobRepository)
                .<UserTagWatchRaw, TagStatDto>chunk(CHUNK_SIZE, transactionManager)
                .reader(tagStatItemReader)
                .processor(tagStatItemProcessor)
                .writer(tagStatItemWriter)
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(10)  // 10건까지 skip 허용 (데이터 이상 방어)
                .build();
    }

    /**
     * Step2: 개인 시청 리포트 집계 → monthly_watch_report upsert
     */
    @Bean("monthlyReportStep")
    public Step monthlyReportStep() {
        return new StepBuilder("monthlyReportStep", jobRepository)
                .<UserWatchDetailRaw, MonthlyReportDto>chunk(CHUNK_SIZE, transactionManager)
                .reader(monthlyReportItemReader)
                .processor(monthlyReportItemProcessor)
                .writer(monthlyReportItemWriter)
                .listener(monthlyReportItemWriter)   // StepExecutionListener: flush
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(10)
                .build();
    }
}