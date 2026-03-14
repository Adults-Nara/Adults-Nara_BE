package com.ott.core.modules.stats.repository;

import com.ott.common.persistence.entity.MonthlyWatchReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MonthlyTagWatchStatsRepository extends JpaRepository<MonthlyWatchReport, Long> {

    /**
     * 사용자의 월간 태그별 통계 조회 (시청 시간 내림차순)
     */
    @Query("""
        SELECT m FROM MonthlyWatchReport m
        WHERE m.userId = :userId
          AND m.statsYear = :year
          AND m.statsMonth = :month
        ORDER BY m.totalWatchSeconds DESC
    """)
    List<MonthlyWatchReport> findByUserIdAndYearMonth(
            @Param("userId") Long userId,
            @Param("year") Integer year,
            @Param("month") Integer month
    );

    /**
     * 특정 월의 통계가 존재하는지 확인
     */
    boolean existsByUserIdAndStatsYearAndStatsMonth(Long userId, Integer statsYear, Integer statsMonth);
}
