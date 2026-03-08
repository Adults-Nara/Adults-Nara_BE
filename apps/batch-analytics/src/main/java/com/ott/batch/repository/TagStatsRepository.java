package com.ott.batch.repository;

import com.ott.common.persistence.entity.TagStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TagStatsRepository extends JpaRepository<TagStats, Long> {

    List<TagStats> findByUserIdAndStatsDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

    // upsertTagStats() 메서드 삭제됨
}