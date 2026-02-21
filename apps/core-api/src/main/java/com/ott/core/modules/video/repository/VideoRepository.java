package com.ott.core.modules.video.repository;

import com.ott.common.persistence.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoRepository extends JpaRepository<Video, Long> {
}
