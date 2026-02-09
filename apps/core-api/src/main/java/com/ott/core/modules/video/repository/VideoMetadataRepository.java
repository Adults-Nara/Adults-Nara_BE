package com.ott.core.modules.video.repository;

import com.ott.common.persistence.entity.VideoMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoMetadataRepository extends JpaRepository<VideoMetadata, Long> {
}
