package com.ott.core.modules.stats.repository;

import com.ott.common.persistence.entity.TagStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * core-api 모듈 전용 TagStats 조회 Repository.
 * 쓰기(배치 upsert)는 batch-analytics 모듈의 TagStatsRepository가 담당.
 * core-api는 조회 전용으로만 사용한다.
 */
public interface TagStatsQueryRepository extends JpaRepository<TagStats, Long> {

    @Query("""
            SELECT ts.tag.id, ts.tag.tagName, SUM(ts.totalViewTime), SUM(ts.viewCount)
            FROM TagStats ts
            WHERE ts.user.id = :userId
              AND ts.statsDate >= :from
              AND ts.statsDate <= :to
            GROUP BY ts.tag.id, ts.tag.tagName
            ORDER BY SUM(ts.totalViewTime) DESC
            """)
    List<Object[]> findTagSummaryByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );
}