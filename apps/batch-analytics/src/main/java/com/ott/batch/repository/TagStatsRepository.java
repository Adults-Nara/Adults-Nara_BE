package com.ott.batch.repository;

import com.ott.common.persistence.entity.TagStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TagStatsRepository extends JpaRepository<TagStats, Long> {

    Optional<TagStats> findByUserIdAndTagIdAndStatsDate(Long userId, Long tagId, LocalDate statsDate);

    List<TagStats> findByUserIdAndStatsDateBetween(Long userId, LocalDate from, LocalDate to);

    /**
     * 특정 사용자의 특정 연월 태그별 집계
     * stats_date가 YYYY-MM-01 ~ YYYY-MM-말일 범위인 레코드를 태그별로 합산
     */
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

    /**
     * 월별 배치 upsert - 날짜+사용자+태그 기준으로 insert or update
     */
    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO tag_stats (tag_stats_id, tag_id, user_id, stats_date, total_view_time, view_count, created_at, updated_at)
            VALUES (:id, :tagId, :userId, :statsDate, :totalViewTime, :viewCount, NOW(), NOW())
            ON CONFLICT (tag_id, user_id, stats_date)
            DO UPDATE SET
                total_view_time = tag_stats.total_view_time + EXCLUDED.total_view_time,
                view_count      = tag_stats.view_count + EXCLUDED.view_count,
                updated_at      = NOW()
            """, nativeQuery = true)
    void upsertTagStats(
            @Param("id") Long id,
            @Param("tagId") Long tagId,
            @Param("userId") Long userId,
            @Param("statsDate") LocalDate statsDate,
            @Param("totalViewTime") int totalViewTime,
            @Param("viewCount") int viewCount
    );
}