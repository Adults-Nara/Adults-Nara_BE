package com.ott.core.modules.stats.controller;

import com.ott.common.response.ApiResponse;
import com.ott.core.modules.stats.dto.MonthlyStatsResponse;
import com.ott.core.modules.stats.service.MonthlyStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "통계 API", description = "마이페이지 시청 통계")
@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
public class MonthlyStatsController {

    private final MonthlyStatsService monthlyStatsService;

    /**
     * 월별 개인 시청 리포트 조회
     *
     * @param yearMonth "yyyy-MM" 형식. 없으면 직전 월 기준
     */
    @Operation(
            summary = "월별 시청 리포트 조회",
            description = "태그별 시청 통계, 시간대 분포, 완주율, 시청 다양성 점수 등 개인화된 시청 리포트를 반환합니다."
    )
    @GetMapping("/monthly")
    public ApiResponse<MonthlyStatsResponse> getMonthlyStats(
            @AuthenticationPrincipal String userId,
            @RequestParam(required = false) String yearMonth
    ) {
        MonthlyStatsResponse response = monthlyStatsService.getMonthlyStats(Long.parseLong(userId), yearMonth);
        return ApiResponse.success(response);
    }
}