package com.ott.batch.monthly;

import com.ott.batch.monthly.dto.MonthlyReportDto;
import com.ott.batch.monthly.dto.TagStatDto;
import com.ott.batch.monthly.step1.MonthlyTagStatsProcessor;
import com.ott.batch.monthly.step1.MonthlyTagStatsReader;
import com.ott.batch.monthly.step1.MonthlyTagStatsWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.YearMonth;

/**
 * 월간 태그별 통계 배치 Job 설정
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MonthlyStatsBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;

    private final MonthlyTagStatsReader monthlyTagStatsReader;
    private final MonthlyTagStatsProcessor monthlyTagStatsProcessor;
    private final MonthlyTagStatsWriter monthlyTagStatsWriter;

    private static final int CHUNK_SIZE = 100;

    /**
     * 월간 통계 Job
     */
    @Bean
    public Job monthlyStatsJob(Step monthlyTagStatsStep) {
        log.info("[monthlyStatsJob] Job 빌드");
        return new JobBuilder("monthlyStatsJob", jobRepository)
                .start(monthlyTagStatsStep)
                .build();
    }

    /**
     * Step: 월간 태그별 통계 집계
     */
    @Bean
    @JobScope
    public Step monthlyTagStatsStep(@Value("#{jobParameters['yearMonth']}") String yearMonth) {
        log.debug("[monthlyTagStatsStep] Step 빌드: yearMonth={}", yearMonth);
        
        return new StepBuilder("monthlyTagStatsStep", jobRepository)
                .<TagStatDto, MonthlyReportDto>chunk(CHUNK_SIZE, platformTransactionManager)
                .reader(monthlyTagStatsReader.reader(YearMonth.parse(yearMonth)))
                .processor(monthlyTagStatsProcessor)
                .writer(monthlyTagStatsWriter)
                .build();
    }
}
