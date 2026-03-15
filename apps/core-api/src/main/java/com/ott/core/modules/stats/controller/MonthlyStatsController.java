package com.ott.core.modules.stats.controller;

import com.ott.common.response.ApiResponse;
import com.ott.core.modules.stats.dto.MonthlyStatsResponse;
import com.ott.core.modules.stats.service.MonthlyStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@Slf4j
@RestController
@RequestMapping("/api/v1/statistics")
@RequiredArgsConstructor
@Tag(name = "통계 API", description = "사용자 시청 통계 조회")
public class MonthlyStatsController {

    private final MonthlyStatsService monthlyStatsService;

    @Operation(
        summary = "월간 태그별 시청 통계 조회",
        description = "사용자의 월간 태그별 시청 통계를 조회합니다. year, month 미제공 시 현재 월 기준으로 조회됩니다."
    )
    @GetMapping("/monthly")
    public ApiResponse<MonthlyStatsResponse> getMonthlyStats(
            Authentication authentication,
            @Parameter(description = "연도 (예: 2026)") @RequestParam(required = false) Integer year,
            @Parameter(description = "월 (1-12)") @RequestParam(required = false) Integer month) {

        Long userId = Long.parseLong(authentication.getName());

        // 파라미터 유효성 검사
        if ((year != null && month == null) || (year == null && month != null)) {
            throw new IllegalArgumentException("year와 month는 함께 제공되거나 모두 생략되어야 합니다.");
        }

        YearMonth targetMonth = (year != null)
                ? YearMonth.of(year, month)
                : YearMonth.now();

        log.info("[getMonthlyStats] userId={}, year={}, month={}", 
                userId, targetMonth.getYear(), targetMonth.getMonthValue());

        MonthlyStatsResponse response = monthlyStatsService.getMonthlyStats(
                userId, 
                targetMonth.getYear(), 
                targetMonth.getMonthValue()
        );

        return ApiResponse.success(response);
    }
}
