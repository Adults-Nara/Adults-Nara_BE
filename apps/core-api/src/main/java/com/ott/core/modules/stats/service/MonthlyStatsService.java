package com.ott.core.modules.stats.service;

import com.ott.common.error.BusinessException;
import com.ott.common.error.ErrorCode;
import com.ott.common.persistence.entity.MonthlyWatchReport;
import com.ott.core.modules.stats.dto.MonthlyStatsResponse;
import com.ott.core.modules.stats.repository.MonthlyWatchReportRepository;
import com.ott.core.modules.usertag.dto.TagWatchStatsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MonthlyStatsService {

    private final MonthlyWatchReportRepository monthlyWatchReportRepository;

    /**
     * 월간 태그별 통계 조회
     */
    public MonthlyStatsResponse getMonthlyStats(Long userId, Integer year, Integer month) {
        // 데이터 조회
        List<MonthlyWatchReport> reports = monthlyWatchReportRepository
                .findByUserIdAndYearMonth(userId, year, month);

        if (reports.isEmpty()) {
            log.warn("월간 리포트가 없습니다: userId={}, year={}, month={}", userId, year, month);
            throw new BusinessException(ErrorCode.MONTHLY_REPORT_NOT_FOUND);
        }

        // TagWatchStatsResponse 변환
        List<TagWatchStatsResponse> tagStats = reports.stream()
                .map(r -> new TagWatchStatsResponse(
                        String.valueOf(r.getTagId()),
                        r.getTagName(),
                        r.getTotalWatchSeconds()
                ))
                .toList();

        // Navigation 정보 생성
        MonthlyStatsResponse.NavigationInfo navigation = createNavigation(year, month);

        return new MonthlyStatsResponse(year, month, tagStats, navigation);
    }

    /**
     * Navigation 정보 생성
     */
    private MonthlyStatsResponse.NavigationInfo createNavigation(Integer year, Integer month) {
        YearMonth current = YearMonth.of(year, month);
        YearMonth prev = current.minusMonths(1);
        YearMonth next = current.plusMonths(1);
        YearMonth now = YearMonth.now();

        // 미래 월은 hasNext = false
        boolean hasNext = !next.isAfter(now);

        return new MonthlyStatsResponse.NavigationInfo(
                prev.getYear(),
                prev.getMonthValue(),
                next.getYear(),
                next.getMonthValue(),
                true,  // 과거는 항상 존재
                hasNext
        );
    }
}
