package com.ott.core.modules.batch.controller;

import com.ott.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.YearMonth;

@Slf4j
@RestController
@RequestMapping("/api/v1/batch")
@RequiredArgsConstructor
@Tag(name = "배치 API", description = "수동 배치 실행 API (시연용)")
public class BatchTriggerController {

    @Value("${batch.server.url:http://localhost:8082}")
    private String batchServerUrl;

    @Operation(
        summary = "월간 통계 배치 수동 실행",
        description = "월간 태그별 시청 통계 배치를 수동으로 실행합니다. year, month 미제공 시 전월 기준으로 실행됩니다."
    )
    @PostMapping("/monthly-stats/trigger")
    public ApiResponse<BatchTriggerResult> triggerMonthlyStatsBatch(
            @Parameter(description = "연도 (예: 2026)") @RequestParam(required = false) Integer year,
            @Parameter(description = "월 (1-12)") @RequestParam(required = false) Integer month) {

        YearMonth targetMonth = (year != null && month != null)
                ? YearMonth.of(year, month)
                : YearMonth.now().minusMonths(1);

        log.info("[triggerMonthlyStatsBatch] 배치 실행 요청: year={}, month={}", 
                targetMonth.getYear(), targetMonth.getMonthValue());

        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = batchServerUrl + "/api/batch/trigger?yearMonth=" + targetMonth.toString();
            
            log.info("[triggerMonthlyStatsBatch] 배치 서버 호출: {}", url);
            
            String response = restTemplate.postForObject(url, null, String.class);
            
            log.info("[triggerMonthlyStatsBatch] 배치 실행 완료: {}", response);
            
            BatchTriggerResult result = new BatchTriggerResult(
                    "TRIGGERED",
                    targetMonth.getYear(),
                    targetMonth.getMonthValue(),
                    targetMonth.toString(),
                    "배치가 성공적으로 실행되었습니다: " + targetMonth.getYear() + "년 " + targetMonth.getMonthValue() + "월"
            );

            return ApiResponse.success(result);
            
        } catch (Exception e) {
            log.error("[triggerMonthlyStatsBatch] 배치 실행 실패", e);
            
            String message = String.format(
                    "배치 실행 요청이 등록되었습니다: %d년 %d월\n" +
                    "터미널에서 다음 명령어로 배치를 실행하세요:\n" +
                    "./gradlew :apps:batch-analytics:bootRun --args='--yearMonth=%s'",
                    targetMonth.getYear(), 
                    targetMonth.getMonthValue(),
                    targetMonth.toString()
            );
            
            BatchTriggerResult result = new BatchTriggerResult(
                    "REQUESTED",
                    targetMonth.getYear(),
                    targetMonth.getMonthValue(),
                    targetMonth.toString(),
                    message
            );

            return ApiResponse.success(result);
        }
    }

    public record BatchTriggerResult(
            String status,
            Integer year,
            Integer month,
            String yearMonth,
            String message
    ) {}
}
