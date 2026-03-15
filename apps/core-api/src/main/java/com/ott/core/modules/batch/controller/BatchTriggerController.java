package com.ott.core.modules.batch.controller;

import com.ott.common.error.BusinessException;
import com.ott.common.error.ErrorCode;
import com.ott.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.YearMonth;

@Slf4j
@RestController
@RequestMapping("/api/v1/batch")
@RequiredArgsConstructor
@Tag(name = "배치 API", description = "수동 배치 실행 API (시연용)")
public class BatchTriggerController {

    private final RestTemplate restTemplate;

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

        // 파라미터 유효성 검사
        if ((year != null && month == null) || (year == null && month != null)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        YearMonth targetMonth = (year != null)
                ? YearMonth.of(year, month)
                : YearMonth.now().minusMonths(1);

        log.info("[triggerMonthlyStatsBatch] 배치 실행 요청: year={}, month={}", 
                targetMonth.getYear(), targetMonth.getMonthValue());

        try {
            String url = batchServerUrl + "/api/batch/trigger?yearMonth=" + targetMonth.toString();
            
            log.info("[triggerMonthlyStatsBatch] 배치 서버 호출: {}", url);
            
            BatchExecutionResponse response = restTemplate.postForObject(url, null, BatchExecutionResponse.class);
            
            log.info("[triggerMonthlyStatsBatch] 배치 실행 완료: jobExecutionId={}, status={}", 
                    response.jobExecutionId(), response.status());
            
            BatchTriggerResult result = new BatchTriggerResult(
                    "TRIGGERED",
                    targetMonth.getYear(),
                    targetMonth.getMonthValue(),
                    targetMonth.toString(),
                    String.format("배치가 성공적으로 실행되었습니다 (Job ID: %d, Status: %s)", 
                            response.jobExecutionId(), response.status())
            );

            return ApiResponse.success(result);
            
        } catch (RestClientException e) {
            log.error("[triggerMonthlyStatsBatch] 배치 서버 호출 실패", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, e);
        }
    }

    // batch-analytics의 응답 DTO (역직렬화용)
    record BatchExecutionResponse(
            Long jobExecutionId,
            String status,
            String yearMonth
    ) {}

    // API 응답 DTO
    public record BatchTriggerResult(
            String status,
            Integer year,
            Integer month,
            String yearMonth,
            String message
    ) {}
}
