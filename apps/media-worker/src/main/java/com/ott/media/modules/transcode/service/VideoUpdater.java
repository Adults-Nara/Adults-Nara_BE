package com.ott.media.modules.transcode.service;

import com.ott.common.error.BusinessException;
import com.ott.common.error.ErrorCode;
import com.ott.common.persistence.entity.Video;
import com.ott.common.persistence.entity.VideoMetadata;
import com.ott.common.persistence.enums.ProcessingStatus;
import com.ott.media.modules.transcode.repository.VideoMetadataRepository;
import com.ott.media.modules.transcode.repository.VideoRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

@Component
public class VideoUpdater {
    private final VideoRepository videoRepository;
    private final VideoMetadataRepository videoMetadataRepository;

    public VideoUpdater(VideoRepository videoRepository,
                        VideoMetadataRepository videoMetadataRepository) {
        this.videoRepository = videoRepository;
        this.videoMetadataRepository = videoMetadataRepository;
    }

    @Transactional
    public void updateReady(Long videoId, int version) {
       Video video = readByVideo(videoId);
       video.markReady(version);
    }

    @Transactional
    public void updateVideoDuration(Long videoId, int duration) {
        VideoMetadata videoMetadata = videoMetadataRepository.findByVideoIdAndDeleted(videoId, false)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        videoMetadata.setDuration(duration);
    }

    public Video readByVideo(Long videoId) {
        return videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("video not found"));
    }

    @Transactional
    public void updateFail(Long videoId) {
        Video video = readByVideo(videoId);
        video.setProcessingStatus(ProcessingStatus.FAILED);
    }
}
