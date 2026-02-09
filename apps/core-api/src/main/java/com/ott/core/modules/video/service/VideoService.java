package com.ott.core.modules.video.service;

import com.ott.common.persistence.enums.ProcessingStatus;
import com.ott.common.persistence.entity.Video;
import com.ott.common.persistence.entity.VideoUploadSession;
import com.ott.common.persistence.enums.UploadSessionStatus;
import com.ott.common.persistence.enums.Visibility;
import com.ott.common.util.IdGenerator;
import com.ott.core.modules.video.dto.multipart.CompletedPartDto;
import com.ott.core.modules.video.dto.multipart.MultipartInitResult;
import com.ott.core.modules.video.repository.VideoRepository;
import com.ott.core.modules.video.repository.VideoUploadSessionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class VideoService {
    private final VideoRepository videoRepository;
    private final VideoUploadSessionRepository sessionRepository;

    private final PresignedMultipartProcessor presignedMultipartProcessor;
    private final ObjectStorageVerifier objectStorageVerifier;

    @Value("${aws.s3.source-bucket}")
    private String bucket;

    public VideoService(VideoRepository videoRepository,
                        VideoUploadSessionRepository sessionRepository,
                        PresignedMultipartProcessor presignedMultipartProcessor,
                        ObjectStorageVerifier objectStorageVerifier) {
        this.videoRepository = videoRepository;
        this.sessionRepository = sessionRepository;
        this.presignedMultipartProcessor = presignedMultipartProcessor;
        this.objectStorageVerifier = objectStorageVerifier;
    }

    @Transactional
    public void updateVisibility(Long videoId, Visibility visibility) {
        Video v = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("video not found"));

        // READY 아닌데 공개 요청이면 차단
        if (visibility == Visibility.PUBLIC && v.getProcessingStatus() != ProcessingStatus.READY) {
            throw new IllegalStateException("Video not ready for publish");
        }

        v.setVisibility(visibility);
    }

    @Transactional
    public MultipartInitResult initMultipartUpload(String contentType, long sizeBytes) {
        if (sizeBytes <= 0) throw new IllegalArgumentException("sizeBytes must be > 0");

        Long videoId = IdGenerator.generate();
        String sourceKey = "videos/" + videoId + "/source/source.mp4";

        videoRepository.save(new Video(videoId, sourceKey));

        MultipartInitResult result = presignedMultipartProcessor.initMultipart(videoId, bucket, sourceKey, contentType, sizeBytes);


        OffsetDateTime expiresAt = OffsetDateTime.ofInstant(
                Instant.ofEpochSecond(result.expiresAtEpochSeconds()),
                ZoneOffset.UTC
        );
        VideoUploadSession session = new VideoUploadSession(
                IdGenerator.generate(),
                videoId,
                bucket,
                sourceKey,
                result.uploadId(),
                result.partSizeBytes(),
                sizeBytes,
                expiresAt
        );
        sessionRepository.save(session);

        return result;
    }

    @Transactional
    public void completeMultipartUpload(Long videoId, String uploadId, List<CompletedPartDto> completedParts, long sizeBytes) {
        Video v = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("video not found"));

        VideoUploadSession session = sessionRepository
                .findFirstByVideoIdAndStatusOrderByCreatedAtDesc(videoId, UploadSessionStatus.UPLOADING)
                .orElseThrow(() -> new IllegalStateException("No active upload session"));

        // uploadId 매칭 검증
        if (!session.getS3UploadId().equals(uploadId)) {
            throw new IllegalArgumentException("uploadId mismatch");
        }

        // 만료 검증
        if (session.isExpiredNow()) {
            throw new IllegalStateException("Upload session expired");
        }

        presignedMultipartProcessor.completeMultipart(bucket, v.getSourceKey(), uploadId, completedParts);

        objectStorageVerifier.verifyExistsAndSize(bucket, v.getSourceKey(), sizeBytes);

        session.markCompleted();
        v.setProcessingStatus(ProcessingStatus.UPLOADED);

        // todo: kafka uploaded 메시지 발송
    }

    @Transactional
    public void abortMultipartUpload(Long videoId, String uploadId) {
        Video v = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("video not found"));

        VideoUploadSession session = sessionRepository
                .findFirstByVideoIdAndStatusOrderByCreatedAtDesc(videoId, UploadSessionStatus.UPLOADING)
                .orElseThrow(() -> new IllegalStateException("No active upload session"));

        if (!session.getS3UploadId().equals(uploadId)) {
            throw new IllegalArgumentException("uploadId mismatch");
        }

        presignedMultipartProcessor.abortMultipart(bucket, v.getSourceKey(), uploadId);
        session.markAborted();
    }
}
