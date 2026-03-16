package com.ott.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${batch.mode:oneshot}")
    private String batchMode;

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(BatchAnalyticsApplication.class, args);
        
        // oneshot 모드인 경우 배치 실행 후 종료
        String mode = context.getEnvironment().getProperty("batch.mode", "oneshot");
        if ("oneshot".equals(mode)) {
            log.info("========================================");
            log.info("배치 처리 완료 - 애플리케이션 종료");
            log.info("========================================");
            System.exit(SpringApplication.exit(context));
        } else {
            log.info("Server 모드로 실행 중 - 종료하지 않음");
        }
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("========================================");
        log.info("월간 통계 배치 실행 시작");
        log.info("========================================");

        // 커맨드 라인 인자 또는 환경변수에서 yearMonth 추출
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
