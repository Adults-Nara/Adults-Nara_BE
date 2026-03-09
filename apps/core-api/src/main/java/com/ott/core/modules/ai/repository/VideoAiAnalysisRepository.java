package com.ott.core.modules.ai.repository;

import com.ott.common.persistence.entity.VideoAiAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VideoAiAnalysisRepository extends JpaRepository<VideoAiAnalysis, Long> {
}
