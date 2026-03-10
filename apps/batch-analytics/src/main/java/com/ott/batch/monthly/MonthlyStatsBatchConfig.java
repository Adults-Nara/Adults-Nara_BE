package com.ott.batch.monthly;

import com.ott.batch.monthly.dto.TagStatDto;
import com.ott.batch.monthly.dto.MonthlyReportDto;
import com.ott.batch.monthly.step1.TagStatReader;
import com.ott.batch.monthly.step1.TagStatWriter;
import com.ott.batch.monthly.step2.MonthlyReportReader;
import com.ott.batch.monthly.step2.MonthlyReportProcessor;
import com.ott.batch.monthly.step2.MonthlyReportWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.OffsetDateTime;

/**
 * 월간 통계 배치 Job 설정
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MonthlyStatsBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;

    private final TagStatReader tagStatReader;
    private final TagStatWriter tagStatWriter;

    private final MonthlyReportReader monthlyReportReader;
    private final MonthlyReportProcessor monthlyReportProcessor;
    private final MonthlyReportWriter monthlyReportWriter;

    private static final int CHUNK_SIZE = 100;

    /**
     * 월간 통계 Job
     */
    @Bean
    public Job monthlyStatsJob(Step monthlyTagStatsStep, Step monthlyReportStep) {
        log.info("[monthlyStatsJob] Job 빌드");
        return new JobBuilder("monthlyStatsJob", jobRepository)
                .start(monthlyTagStatsStep)
                .next(monthlyReportStep)
                .build();
    }

    /**
     * Step 1: 태그별 통계 집계
     */
    @Bean
    @JobScope
    public Step monthlyTagStatsStep() {
        log.debug("[monthlyTagStatsStep] Step 빌드");
        return new StepBuilder("monthlyTagStatsStep", jobRepository)
                .<TagStatDto, TagStatDto>chunk(CHUNK_SIZE, platformTransactionManager)
                .reader(tagStatItemReader(null, null))
                .writer(tagStatWriter)
                .build();
    }

    /**
     * Step 2: 사용자별 월간 리포트 생성
     */
    @Bean
    @JobScope
    public Step monthlyReportStep() {
        log.debug("[monthlyReportStep] Step 빌드");
        return new StepBuilder("monthlyReportStep", jobRepository)
                .<Long, MonthlyReportDto>chunk(CHUNK_SIZE, platformTransactionManager)
                .reader(monthlyReportItemReader(null))
                .processor(monthlyReportProcessor)
                .writer(monthlyReportWriter)
                .build();
    }

    /**
     * TagStat Reader
     */
    @Bean
    @StepScope
    public ItemReader<TagStatDto> tagStatItemReader(
            @Value("#{jobParameters['rangeFrom']}") String rangeFrom,
            @Value("#{jobParameters['rangeTo']}") String rangeTo) {
        log.info("[tagStatItemReader] Reader 생성: {} ~ {}", rangeFrom, rangeTo);

        OffsetDateTime from = OffsetDateTime.parse(rangeFrom);
        OffsetDateTime to = OffsetDateTime.parse(rangeTo);

        return tagStatReader.reader(from, to);
    }

    /**
     * MonthlyReport Reader
     */
    @Bean
    @StepScope
    public ItemReader<Long> monthlyReportItemReader(
            @Value("#{jobParameters['yearMonth']}") String yearMonth) {
        log.info("[monthlyReportItemReader] Reader 생성: yearMonth={}", yearMonth);

        return monthlyReportReader.reader(yearMonth);
    }
}