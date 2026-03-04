package com.ott.core.modules.stats.service;

import com.ott.common.error.BusinessException;
import com.ott.common.error.ErrorCode;
import com.ott.common.persistence.entity.MonthlyWatchReport;
import com.ott.core.modules.stats.repository.MonthlyWatchReportQueryRepository;
import com.ott.core.modules.stats.repository.TagStatsQueryRepository;
import com.ott.core.modules.stats.dto.MonthlyStatsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MonthlyStatsService {

    private final MonthlyWatchReportQueryRepository monthlyWatchReportRepository;
    private final TagStatsQueryRepository tagStatsRepository;

    private static final DateTimeFormatter YEAR_MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * 특정 연월의 개인 시청 리포트 조회.
     *
     * @param userId         사용자 ID
     * @param yearMonthStr   "yyyy-MM" 형식. null이면 전월(가장 최근 집계) 반환
     */
    public MonthlyStatsResponse getMonthlyStats(Long userId, String yearMonthStr) {
        String targetYearMonth = resolveYearMonth(yearMonthStr);

        MonthlyWatchReport report = monthlyWatchReportRepository
                .findByUserIdAndReportYearMonth(userId, targetYearMonth)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        // 태그별 상세 통계: tag_stats 테이블에서 해당 월 조회
        YearMonth ym = YearMonth.parse(targetYearMonth, YEAR_MONTH_FMT);
        LocalDate from = ym.atDay(1);
        LocalDate to   = ym.atEndOfMonth();

        List<Object[]> tagSummaryRows = tagStatsRepository.findTagSummaryByUserIdAndDateRange(userId, from, to);

        List<MonthlyStatsResponse.TagStatSummary> tagStats = tagSummaryRows.stream()
                .map(row -> MonthlyStatsResponse.TagStatSummary.builder()
                        .tagName((String) row[1])
                        .totalViewSeconds(((Number) row[2]).longValue())
                        .viewCount(((Number) row[3]).intValue())
                        .build())
                .toList();

        return MonthlyStatsResponse.from(report, tagStats);
    }

    /**
     * yearMonthStr이 null이면 전월을 반환한다.
     */
    private String resolveYearMonth(String yearMonthStr) {
        if (yearMonthStr != null && !yearMonthStr.isBlank()) {
            return yearMonthStr;
        }
        // 기본값: 전월
        return YearMonth.now().minusMonths(1).format(YEAR_MONTH_FMT);
    }
}