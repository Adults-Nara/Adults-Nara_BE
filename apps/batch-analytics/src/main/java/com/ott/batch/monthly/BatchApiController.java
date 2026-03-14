package com.ott.batch.monthly;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.YearMonth;

@Slf4j
@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
public class BatchApiController {

    private final JobLauncher jobLauncher;
    private final Job monthlyStatsJob;

    @PostMapping("/trigger")
    public BatchExecutionResponse triggerBatch(@RequestParam String yearMonth) {
        try {
            log.info("[BatchApiController] 배치 실행 요청: yearMonth={}", yearMonth);
            
            YearMonth ym = YearMonth.parse(yearMonth);
            OffsetDateTime rangeFrom = ym.atDay(1).atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
            OffsetDateTime rangeTo = ym.plusMonths(1).atDay(1).atStartOfDay().atOffset(OffsetDateTime.now().getOffset());

            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("yearMonth", yearMonth)
                    .addString("rangeFrom", rangeFrom.toString())
                    .addString("rangeTo", rangeTo.toString())
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            JobExecution jobExecution = jobLauncher.run(monthlyStatsJob, jobParameters);

            log.info("[BatchApiController] 배치 실행 완료: status={}", jobExecution.getStatus());

            return new BatchExecutionResponse(
                    jobExecution.getJobId(),
                    jobExecution.getStatus().name(),
                    yearMonth
            );
            
        } catch (JobExecutionAlreadyRunningException | JobRestartException | 
                 JobInstanceAlreadyCompleteException | JobParametersInvalidException e) {
            log.error("[BatchApiController] 배치 실행 실패", e);
            throw new RuntimeException("배치 실행 실패: " + e.getMessage());
        }
    }

    public record BatchExecutionResponse(
            Long jobExecutionId,
            String status,
            String yearMonth
    ) {}
}
