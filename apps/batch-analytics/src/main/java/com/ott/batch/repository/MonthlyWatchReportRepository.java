package com.ott.batch.repository;

import com.ott.common.persistence.entity.MonthlyWatchReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MonthlyWatchReportRepository extends JpaRepository<MonthlyWatchReport, Long> {

    Optional<MonthlyWatchReport> findByUserIdAndReportYearMonth(Long userId, String reportYearMonth);

}