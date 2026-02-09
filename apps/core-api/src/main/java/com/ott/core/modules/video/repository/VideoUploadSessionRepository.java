package com.ott.core.modules.video.repository;

import com.ott.common.persistence.enums.UploadSessionStatus;
import com.ott.common.persistence.entity.VideoUploadSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface VideoUploadSessionRepository extends JpaRepository<VideoUploadSession, Long> {
    Optional<VideoUploadSession> findFirstByVideoIdAndStatusOrderByCreatedAtDesc(Long videoId, UploadSessionStatus status);

    List<VideoUploadSession> findTop100ByStatusAndExpiresAtBeforeOrderByExpiresAtAsc(UploadSessionStatus status, OffsetDateTime before);

}
