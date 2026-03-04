package com.ott.batch.repository;

import com.ott.common.persistence.entity.VideoMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VideoMetadataRepository extends JpaRepository<VideoMetadata, Long> {
    Optional<VideoMetadata> findByVideoId(Long videoId);
}