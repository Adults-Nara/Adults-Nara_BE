package com.ott.core.modules.stats.repository;

import com.ott.common.persistence.entity.TagStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TagStatsQueryRepository extends JpaRepository<TagStats, Long> {

    // findTagSummaryByUserIdAndDateRange() 메서드 삭제됨
}