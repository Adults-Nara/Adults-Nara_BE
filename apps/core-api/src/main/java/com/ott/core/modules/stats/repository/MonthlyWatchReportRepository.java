package com.ott.core.modules.stats.repository;

import com.ott.common.persistence.entity.MonthlyWatchReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MonthlyWatchReportRepository extends JpaRepository<MonthlyWatchReport, Long> {

    /**
     * 사용자 ID와 연월로 리포트 조회
     */
    Optional<MonthlyWatchReport> findByUserIdAndReportYearMonth(Long userId, String reportYearMonth);
}