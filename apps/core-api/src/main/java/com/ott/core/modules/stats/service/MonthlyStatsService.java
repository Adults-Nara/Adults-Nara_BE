package com.ott.core.modules.stats.service;

import com.ott.core.modules.stats.repository.MonthlyWatchReportRepository;
import com.ott.common.persistence.entity.MonthlyWatchReport;
import com.ott.core.modules.stats.dto.MonthlyStatsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 월간 통계 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonthlyStatsService {

    private final MonthlyWatchReportRepository monthlyWatchReportRepository;

    /**
     * 월간 리포트 조회
     */
    public MonthlyStatsResponse getMonthlyReport(Long userId, String yearMonth) {
        log.info("월간 리포트 조회: userId={}, yearMonth={}", userId, yearMonth);

        MonthlyWatchReport report = monthlyWatchReportRepository
                .findByUserIdAndReportYearMonth(userId, yearMonth)
                .orElseThrow(() -> new RuntimeException("월간 리포트가 없습니다: " + yearMonth));

        return MonthlyStatsResponse.from(report);
    }
}