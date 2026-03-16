package com.ott.core.modules.stats.repository;

import com.ott.common.persistence.entity.MonthlyWatchReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MonthlyWatchReportRepository extends JpaRepository<MonthlyWatchReport, Long> {

    /**
     * 특정 사용자의 특정 년월 통계 조회
     */
    List<MonthlyWatchReport> findByUserIdAndStatsYearAndStatsMonth(
            Long userId,
            Integer statsYear,
            Integer statsMonth
    );

    /**
     * 특정 사용자의 특정 년월 통계 존재 여부 확인 (Navigation용)
     */
    boolean existsByUserIdAndStatsYearAndStatsMonth(
            Long userId,
            Integer statsYear,
            Integer statsMonth
    );
}