package com.ott.core.modules.video.service;

import com.ott.common.persistence.entity.VideoMetadata;
import com.ott.core.modules.video.repository.VideoMetadataRepository;
import com.ott.core.modules.video.repository.VideoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class UploadCleanupScheduler {
    private final VideoRepository videoRepository;
    private final VideoMetadataRepository videoMetadataRepository;
    private final S3ObjectStorage s3ObjectStorage;

    @Value("${aws.s3.source-bucket}")
    private String bucket;

    @Scheduled(cron = "0 */1 * * * *", zone = "Asia/Seoul") // 15분마다
    @Transactional
    public void cleanup() {
        // 하루 전 ~ 하루 전 + 1시간
        OffsetDateTime after = OffsetDateTime.now().minusDays(1);
        OffsetDateTime before = after.plusHours(1);

        List<VideoMetadata> cleanupTargets = videoMetadataRepository.findAllByTitleIsNullAndCreatedAtBetweenAndDeletedIsFalse(after, before);

        List<Long> videoIds = cleanupTargets.stream()
                .map(VideoMetadata::getVideoId)
                .collect(Collectors.toList());

        videoMetadataRepository.softDeleteByAdmin(videoIds);
        videoRepository.softDeleteByIds(videoIds);

        for (Long videoId : videoIds) {
            s3ObjectStorage.deleteByPrefix(bucket, "videos/" + videoId + "/");
        }
    }
}
