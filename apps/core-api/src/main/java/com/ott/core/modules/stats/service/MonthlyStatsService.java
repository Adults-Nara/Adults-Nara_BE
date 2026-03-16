package com.ott.core.modules.stats.service;

import com.ott.common.persistence.entity.MonthlyWatchReport;
import com.ott.core.modules.stats.dto.MonthlyStatsResponse;
import com.ott.core.modules.stats.dto.TagStatsDto;
import com.ott.core.modules.stats.repository.MonthlyWatchReportRepository;
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
        List<MonthlyWatchReport> reports = monthlyWatchReportRepository
                .findByUserIdAndStatsYearAndStatsMonthOrderByTotalWatchSecondsDesc(userId, year, month);

        List<TagStatsDto> tags = reports.stream()
                .map(report -> new TagStatsDto(
                        report.getTagId(),
                        report.getTagName(),
                        report.getTotalWatchSeconds(),
                        report.getWatchCount()
                ))
                .toList();

        YearMonth currentMonth = YearMonth.of(year, month);
        YearMonth previousMonth = currentMonth.minusMonths(1);
        YearMonth nextMonth = currentMonth.plusMonths(1);
        YearMonth now = YearMonth.now();

        boolean hasPrevious = monthlyWatchReportRepository
                .existsByUserIdAndStatsYearAndStatsMonth(userId, previousMonth.getYear(), previousMonth.getMonthValue());
        boolean hasNext = !nextMonth.isAfter(now) && monthlyWatchReportRepository
                .existsByUserIdAndStatsYearAndStatsMonth(userId, nextMonth.getYear(), nextMonth.getMonthValue());

        return MonthlyStatsResponse.builder()
                .year(year)
                .month(month)
                .userId(userId)
                .tags(tags)
                .hasPrevious(hasPrevious)
                .hasNext(hasNext)
                .previousMonth(hasPrevious ? previousMonth.toString() : null)
                .nextMonth(hasNext ? nextMonth.toString() : null)
                .build();
    }
}
