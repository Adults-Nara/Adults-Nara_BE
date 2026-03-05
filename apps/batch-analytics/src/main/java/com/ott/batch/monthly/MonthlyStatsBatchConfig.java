package com.ott.batch.monthly;

import com.ott.batch.monthly.dto.MonthlyReportDto;
import com.ott.batch.monthly.dto.TagStatDto;
import com.ott.batch.monthly.step1.TagStatProcessor;
import com.ott.batch.monthly.step1.TagStatReader;
import com.ott.batch.monthly.step1.TagStatWriter;
import com.ott.batch.monthly.step2.MonthlyReportProcessor;
import com.ott.batch.monthly.step2.MonthlyReportReader;
import com.ott.batch.monthly.step2.MonthlyReportWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.OffsetDateTime;

@Slf4j
@Configuration
public class MonthlyStatsBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final TagStatReader tagStatReader;
    private final TagStatProcessor tagStatProcessor;
    private final TagStatWriter tagStatWriter;
    private final MonthlyReportReader monthlyReportReader;
    private final MonthlyReportProcessor monthlyReportProcessor;
    private final MonthlyReportWriter monthlyReportWriter;

    public MonthlyStatsBatchConfig(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            TagStatReader tagStatReader,
            TagStatProcessor tagStatProcessor,
            TagStatWriter tagStatWriter,
            MonthlyReportReader monthlyReportReader,
            MonthlyReportProcessor monthlyReportProcessor,
            MonthlyReportWriter monthlyReportWriter) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.tagStatReader = tagStatReader;
        this.tagStatProcessor = tagStatProcessor;
        this.tagStatWriter = tagStatWriter;
        this.monthlyReportReader = monthlyReportReader;
        this.monthlyReportProcessor = monthlyReportProcessor;
        this.monthlyReportWriter = monthlyReportWriter;
    }

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
    @StepScope
    public JdbcCursorItemReader<TagStatDto> tagStatItemReader(
            @Value("#{jobParameters['rangeFrom']}") String rangeFromStr,
            @Value("#{jobParameters['rangeTo']}") String rangeToStr) {

        OffsetDateTime rangeFrom = OffsetDateTime.parse(rangeFromStr);
        OffsetDateTime rangeTo = OffsetDateTime.parse(rangeToStr);

        log.info("[tagStatItemReader] Reader 생성: {} ~ {}", rangeFrom, rangeTo);
        return tagStatReader.reader(rangeFrom, rangeTo);
    }

    @Bean
    public Step monthlyTagStatsStep(JdbcCursorItemReader<TagStatDto> tagStatItemReader) {
        return new StepBuilder("monthlyTagStatsStep", jobRepository)
                .<TagStatDto, TagStatDto>chunk(100, transactionManager)
                .reader(tagStatItemReader)
                .processor(tagStatProcessor)
                .writer(tagStatWriter)
                .faultTolerant()
                .skip(DataIntegrityViolationException.class)  // DB 제약조건 위반만 skip
                .skip(EmptyResultDataAccessException.class)   // 데이터 없음만 skip
                .skipLimit(10)
                .build();
    }

    /**
     * Step 2: 월간 리포트 생성
     */
    @Bean
    @StepScope
    public JdbcCursorItemReader<Long> monthlyReportItemReader(
            @Value("#{jobParameters['yearMonth']}") String yearMonth) {

        log.info("[monthlyReportItemReader] Reader 생성: yearMonth={}", yearMonth);
        return monthlyReportReader.reader(yearMonth);
    }

    @Bean
    public Step monthlyReportStep(JdbcCursorItemReader<Long> monthlyReportItemReader) {
        return new StepBuilder("monthlyReportStep", jobRepository)
                .<Long, MonthlyReportDto>chunk(50, transactionManager)
                .reader(monthlyReportItemReader)
                .processor(monthlyReportProcessor)  // ItemProcessor<Long, MonthlyReportDto>
                .writer(monthlyReportWriter)
                .listener(monthlyReportWriter)
                .listener(monthlyReportProcessor)  // StepExecutionListener 추가
                .faultTolerant()
                .skip(DataIntegrityViolationException.class)
                .skip(EmptyResultDataAccessException.class)
                .skipLimit(10)
                .build();
    }
}