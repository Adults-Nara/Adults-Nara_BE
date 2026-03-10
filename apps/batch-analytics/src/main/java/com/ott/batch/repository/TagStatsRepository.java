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
     * N+1 문제 해결: Tag를 JOIN FETCH로 즉시 로딩
     */
    @Query("SELECT ts FROM TagStats ts JOIN FETCH ts.tag t WHERE ts.user.id = :userId AND ts.statsDate BETWEEN :startDate AND :endDate")
    List<TagStats> findByUserIdAndStatsDateBetweenWithTag(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}