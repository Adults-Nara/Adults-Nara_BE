package com.ott.core.modules.stats.controller;

import com.ott.common.response.ApiResponse;
import com.ott.core.modules.stats.dto.MonthlyStatsResponse;
import com.ott.core.modules.stats.service.MonthlyStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;

@Slf4j
@RestController
@RequestMapping("/api/v1/statistics")
@RequiredArgsConstructor
@Tag(name = "통계 API", description = "사용자 시청 통계 관련 API")
public class MonthlyStatsController {

    private final MonthlyStatsService monthlyStatsService;

    @Operation(
        summary = "월간 태그별 시청 통계 조회",
        description = "사용자의 월간 태그별 시청 시간 통계를 조회합니다. year, month 미제공 시 현재 월 기준으로 조회합니다."
    )
    @GetMapping("/monthly")
    public ApiResponse<MonthlyStatsResponse> getMonthlyStats(
            @AuthenticationPrincipal String userIdStr,
            @Parameter(description = "연도 (예: 2026)") @RequestParam(required = false) Integer year,
            @Parameter(description = "월 (1-12)") @RequestParam(required = false) Integer month) {

        // userId 파싱
        Long userId;
        try {
            userId = Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            log.warn("Invalid user ID format in principal: {}", userIdStr.replaceAll("[\\r\\n]", ""));
            throw new IllegalArgumentException("Invalid user ID");
        }

        // year, month 기본값 처리 (현재 월)
        YearMonth targetMonth = (year != null && month != null)
                ? YearMonth.of(year, month)
                : YearMonth.now();

        log.debug("[getMonthlyStats] userId={}, year={}, month={}", userId, targetMonth.getYear(), targetMonth.getMonthValue());

        MonthlyStatsResponse response = monthlyStatsService.getMonthlyStats(
                userId, targetMonth.getYear(), targetMonth.getMonthValue());

        return ApiResponse.success(response);
    }
}
