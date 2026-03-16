package com.ott.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.time.YearMonth;
import java.util.List;

@Slf4j
@SpringBootApplication(scanBasePackages = {
        "com.ott.batch",
        "com.ott.common"
})
@EntityScan(basePackages = "com.ott.common.persistence.entity")
@EnableJpaRepositories(basePackages = "com.ott.batch.repository")
@RequiredArgsConstructor
public class BatchAnalyticsApplication implements ApplicationRunner {

    private final JobLauncher jobLauncher;
    private final Job monthlyStatsJob;

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(BatchAnalyticsApplication.class, args);
        
        log.info("========================================");
        log.info("배치 처리 완료 - 애플리케이션 종료");
        log.info("========================================");
        
        // 배치 실행 후 무조건 종료
        System.exit(SpringApplication.exit(context));
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("========================================");
        log.info("월간 통계 배치 실행 시작");
        log.info("========================================");

        // 환경변수 또는 커맨드 라인에서 yearMonth 추출 (기본값: 전월)
        List<String> yearMonthValues = args.getOptionValues("yearMonth");
        String yearMonth = (yearMonthValues != null && !yearMonthValues.isEmpty())
                ? yearMonthValues.get(0)
                : System.getenv().getOrDefault("YEAR_MONTH", YearMonth.now().minusMonths(1).toString());

        log.info("[BatchAnalyticsApplication] 대상 기간: {}", yearMonth);

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("yearMonth", yearMonth)
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        try {
            JobExecution jobExecution = jobLauncher.run(monthlyStatsJob, jobParameters);

            log.info("[BatchAnalyticsApplication] 배치 실행 완료: status={}", jobExecution.getStatus());

            if (jobExecution.getStatus() != BatchStatus.COMPLETED) {
                log.error("[BatchAnalyticsApplication] 배치 실행 실패! status={}", jobExecution.getStatus());
                
                List<Throwable> failureExceptions = jobExecution.getAllFailureExceptions();
                failureExceptions.forEach(e -> log.error("배치 실패 상세 원인:", e));

                RuntimeException exceptionToThrow = new RuntimeException("배치 실행 실패: " + jobExecution.getStatus());
                if (!failureExceptions.isEmpty()) {
                    exceptionToThrow.initCause(failureExceptions.get(0));
                }
                throw exceptionToThrow;
            }
        } catch (JobInstanceAlreadyCompleteException e) {
            log.info("[BatchAnalyticsApplication] 이미 완료된 배치 작업입니다. yearMonth={}", yearMonth);
        }
    }
}
