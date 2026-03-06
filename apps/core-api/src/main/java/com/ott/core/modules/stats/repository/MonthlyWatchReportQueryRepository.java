package com.ott.core.modules.stats.repository;

import com.ott.common.persistence.entity.MonthlyWatchReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * core-api 모듈 전용 MonthlyWatchReport 조회 Repository.
 * 쓰기(배치 upsert)는 batch-analytics 모듈의 MonthlyWatchReportRepository가 담당.
 * core-api는 조회 전용으로만 사용한다.
 */
public interface MonthlyWatchReportQueryRepository extends JpaRepository<MonthlyWatchReport, Long> {

    Optional<MonthlyWatchReport> findByUserIdAndReportYearMonth(Long userId, String reportYearMonth);
}