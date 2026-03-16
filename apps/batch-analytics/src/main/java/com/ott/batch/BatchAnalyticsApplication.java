package com.ott.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.time.YearMonth;

@Slf4j
@SpringBootApplication(scanBasePackages = {
        "com.ott.batch",
        "com.ott.common"
})
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
        String yearMonth = args.containsOption("yearMonth")
                ? args.getOptionValues("yearMonth").get(0)
                : System.getenv().getOrDefault("YEAR_MONTH", YearMonth.now().minusMonths(1).toString());

        log.info("[BatchAnalyticsApplication] 대상 기간: {}", yearMonth);

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("yearMonth", yearMonth)
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        JobExecution jobExecution = jobLauncher.run(monthlyStatsJob, jobParameters);

        log.info("[BatchAnalyticsApplication] 배치 실행 완료: status={}", jobExecution.getStatus());

        if (jobExecution.getStatus() != BatchStatus.COMPLETED) {
            log.error("[BatchAnalyticsApplication] 배치 실행 실패!");
            throw new RuntimeException("배치 실행 실패: " + jobExecution.getStatus());
        }
    }
}
