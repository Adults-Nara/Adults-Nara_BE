package com.ott.core.modules.stats.service;

import com.ott.common.persistence.entity.MonthlyWatchReport;
import com.ott.core.modules.stats.dto.MonthlyStatsResponse;
import com.ott.core.modules.stats.repository.MonthlyWatchReportRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonthlyStatsService {

    private final MonthlyWatchReportRepository monthlyWatchReportRepository;

    /**
     * 월간 리포트 조회
     *
     * EntityNotFoundException은 Spring의 기본 ExceptionHandler에 의해 404로 변환됨
     */
    @Transactional(readOnly = true)
    public MonthlyStatsResponse getMonthlyReport(Long userId, String yearMonth) {
        MonthlyWatchReport report = monthlyWatchReportRepository
                .findByUserIdAndReportYearMonth(userId, yearMonth)
                .orElseThrow(() -> new EntityNotFoundException("월간 리포트가 없습니다: " + yearMonth));

        return MonthlyStatsResponse.from(report);
    }
}