package com.ott.core.modules.stats.controller;

import com.ott.core.modules.stats.dto.MonthlyStatsResponse;
import com.ott.core.modules.stats.service.MonthlyStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 월간 통계 API Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/statistics/monthly")
@RequiredArgsConstructor
public class MonthlyStatsController {

    private final MonthlyStatsService monthlyStatsService;

    /**
     * 월간 리포트 조회
     *
     * GET /api/v1/statistics/monthly/{yearMonth}
     *
     * @param userIdStr 인증된 사용자 ID (JWT에서 추출, String)
     * @param yearMonth "2026-03" 형식
     */
    @GetMapping("/{yearMonth}")
    public ResponseEntity<MonthlyStatsResponse> getMonthlyReport(
            @AuthenticationPrincipal String userIdStr,
            @PathVariable String yearMonth) {

        Long userId = Long.parseLong(userIdStr);
        log.info("월간 리포트 조회: userId={}, yearMonth={}", userId, yearMonth);

        MonthlyStatsResponse response = monthlyStatsService.getMonthlyReport(userId, yearMonth);

        return ResponseEntity.ok(response);
    }
}