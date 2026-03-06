package com.ott.batch.repository;

import com.ott.common.persistence.entity.TagStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TagStatsRepository extends JpaRepository<TagStats, Long> {

    /**
     * 사용자 ID와 날짜 범위로 태그 통계 조회
     * 🔧 FIX: ts.userId → ts.user.id (엔티티가 User 객체를 가지는 경우)
     */
    @Query("""
        SELECT ts FROM TagStats ts
        WHERE ts.user.id = :userId
          AND ts.statsDate BETWEEN :startDate AND :endDate
        ORDER BY ts.statsDate, ts.totalViewTime DESC
    """)
    List<TagStats> findByUserIdAndStatsDateBetween(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 태그별 통계 UPSERT
     *
     * 🔴 FIX: 누적(+=) → 대체(=)로 변경하여 멱등성 보장
     */
    @Transactional
    @Modifying
    @Query(value = """
        INSERT INTO tag_stats (tag_stats_id, user_id, tag_id, stats_date, total_view_time, view_count, completed_count, created_at)
        VALUES (:tagStatsId, :userId, :tagId, :statsDate, :totalViewTime, :viewCount, :completedCount, NOW())
        ON CONFLICT (user_id, tag_id, stats_date) DO UPDATE SET
                total_view_time = EXCLUDED.total_view_time,
                view_count      = EXCLUDED.view_count,
                completed_count = EXCLUDED.completed_count,
                updated_at      = NOW()
        """, nativeQuery = true)
    void upsertTagStats(
            @Param("tagStatsId") Long tagStatsId,
            @Param("tagId") Long tagId,
            @Param("userId") Long userId,
            @Param("statsDate") LocalDate statsDate,
            @Param("totalViewTime") Long totalViewTime,
            @Param("viewCount") Integer viewCount,
            @Param("completedCount") Integer completedCount
    );
}