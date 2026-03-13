package com.ott.batch.repository;

import com.ott.common.persistence.entity.TagStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TagStatsRepository extends JpaRepository<TagStats, Long> {

    /**
     * 단일 사용자의 태그 통계 조회 (Tag JOIN FETCH)
     */
    @Query("""
        SELECT ts FROM TagStats ts
        JOIN FETCH ts.tag
        WHERE ts.user.id = :userId
          AND ts.statsDate >= :startDate
          AND ts.statsDate <= :endDate
    """)
    List<TagStats> findByUserIdAndStatsDateBetweenWithTag(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 여러 사용자의 태그 통계 일괄 조회 (N+1 방지)
     */
    @Query("""
        SELECT ts FROM TagStats ts
        JOIN FETCH ts.tag t
        LEFT JOIN FETCH t.parent
        JOIN FETCH ts.user
        WHERE ts.user.id IN :userIds
          AND ts.statsDate >= :startDate
          AND ts.statsDate <= :endDate
    """)
    List<TagStats> findByUserIdInAndStatsDateBetween(
            @Param("userIds") List<Long> userIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}