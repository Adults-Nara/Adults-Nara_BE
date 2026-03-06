package com.ott.core.modules.ai.repository;

import com.ott.common.persistence.entity.VideoAiAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VideoAiAnalysisRepository extends JpaRepository<VideoAiAnalysis, Long> {
    Optional<VideoAiAnalysis> findByVideoId(Long videoId);
}
