package com.ott.media.modules.transcode.service;

import com.ott.common.persistence.entity.Video;
import com.ott.media.modules.transcode.repository.VideoRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

@Component
public class VideoUpdater {
    private final VideoRepository videoRepository;

    public VideoUpdater(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    @Transactional
    public void updateReady(Long videoId, int version) {
       Video video = readByVideo(videoId);
       video.markReady(version);
    }

    public Video readByVideo(Long videoId) {
        return videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("video not found"));
    }
}
