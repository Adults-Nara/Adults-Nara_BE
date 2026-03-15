package com.ott.core.modules.stats.service;

import com.ott.common.error.BusinessException;
import com.ott.common.error.ErrorCode;
import com.ott.common.persistence.entity.MonthlyWatchReport;
import com.ott.core.modules.stats.repository.MonthlyWatchReportRepository;
import com.ott.core.modules.stats.dto.MonthlyStatsResponse;
import com.ott.core.modules.usertag.dto.TagWatchStatsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MonthlyStatsService {

    private final MonthlyWatchReportRepository monthlyWatchReportRepository;

    public MonthlyStatsResponse getMonthlyStats(Long userId, Integer year, Integer month) {
        log.info("[getMonthlyStats] userId={}, year={}, month={}", userId, year, month);

        List<MonthlyWatchReport> reports = monthlyWatchReportRepository
                .findByUserIdAndYearMonth(userId, year, month);

        if (reports.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        List<TagWatchStatsResponse> tagStats = reports.stream()
                .map(r -> new TagWatchStatsResponse(
                        r.getTagId().toString(),
                        r.getTagName(),
                        r.getTotalWatchSeconds()
                ))
                .toList();

        YearMonth current = YearMonth.of(year, month);
        YearMonth prev = current.minusMonths(1);
        YearMonth next = current.plusMonths(1);
        YearMonth now = YearMonth.now();

        // 이전 월 데이터 존재 여부 확인
        boolean hasPrev = monthlyWatchReportRepository.existsByUserIdAndStatsYearAndStatsMonth(
                userId, prev.getYear(), prev.getMonthValue()
        );

        MonthlyStatsResponse.NavigationInfo navigation = new MonthlyStatsResponse.NavigationInfo(
                prev.getYear(),
                prev.getMonthValue(),
                next.getYear(),
                next.getMonthValue(),
                hasPrev,
                next.isBefore(now) || next.equals(now)
        );

        return new MonthlyStatsResponse(year, month, tagStats, navigation);
    }
}
