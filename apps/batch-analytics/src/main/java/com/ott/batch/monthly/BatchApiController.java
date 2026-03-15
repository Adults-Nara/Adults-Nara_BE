package com.ott.batch.monthly;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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

            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("yearMonth", yearMonth)
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            JobExecution jobExecution = jobLauncher.run(monthlyStatsJob, jobParameters);

            log.info("[BatchApiController] 배치 실행 완료: status={}", jobExecution.getStatus());

            return new BatchExecutionResponse(
                    jobExecution.getJobId(),
                    jobExecution.getStatus().name(),
                    yearMonth
            );
            
        } catch (JobExecutionAlreadyRunningException | JobInstanceAlreadyCompleteException e) {
            log.warn("[BatchApiController] 배치 실행 불가 (이미 실행 중이거나 완료됨): {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 실행 중이거나 완료된 배치 작업입니다.");
        } catch (JobRestartException | JobParametersInvalidException e) {
            log.error("[BatchApiController] 배치 실행 실패 (잘못된 파라미터 또는 재시작 불가)", e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "배치 파라미터가 유효하지 않거나 재시작할 수 없는 작업입니다.");
        }
    }

    public record BatchExecutionResponse(
            Long jobExecutionId,
            String status,
            String yearMonth
    ) {}
}
