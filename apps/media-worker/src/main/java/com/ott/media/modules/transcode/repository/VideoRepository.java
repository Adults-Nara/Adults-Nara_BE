package com.ott.media.modules.transcode.repository;

import com.ott.common.persistence.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoRepository extends JpaRepository<Video, Long> {
}
