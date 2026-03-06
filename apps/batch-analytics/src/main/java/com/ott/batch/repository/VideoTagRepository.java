package com.ott.batch.repository;

import com.ott.common.persistence.entity.VideoTag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoTagRepository extends JpaRepository<VideoTag, Long> {
}