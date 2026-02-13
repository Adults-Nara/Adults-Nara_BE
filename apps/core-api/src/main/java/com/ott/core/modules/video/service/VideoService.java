package com.ott.core.modules.video.service;

import com.ott.common.persistence.enums.ProcessingStatus;
import com.ott.common.persistence.entity.Video;
import com.ott.common.persistence.entity.VideoUploadSession;
import com.ott.common.persistence.enums.UploadSessionStatus;
import com.ott.common.persistence.enums.Visibility;
import com.ott.common.util.IdGenerator;
import com.ott.core.modules.video.dto.PlayResult;
import com.ott.core.modules.video.dto.multipart.CompletedPartDto;
import com.ott.core.modules.video.dto.multipart.MultipartInitResult;
import com.ott.core.modules.video.event.VideoTranscodeRequestedEvent;
import com.ott.core.modules.video.repository.VideoRepository;
import com.ott.core.modules.video.repository.VideoUploadSessionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@Service
public class VideoService {
    private final VideoRepository videoRepository;
    private final VideoUploadSessionRepository sessionRepository;

    private final PresignedMultipartProcessor presignedMultipartProcessor;
    private final ObjectStorageVerifier objectStorageVerifier;

    private final ApplicationEventPublisher eventPublisher;

    private final SignedCookieProcessor signedCookieProcessor;
    private final String CLOUD_FRONT_DOMAIN;
    private final int COOKIE_TTL_SECONDS;

    private final String BUCKET;

    public VideoService(VideoRepository videoRepository,
                        VideoUploadSessionRepository sessionRepository,
                        PresignedMultipartProcessor presignedMultipartProcessor,
                        ObjectStorageVerifier objectStorageVerifier,
                        ApplicationEventPublisher eventPublisher,
                        SignedCookieProcessor signedCookieProcessor,
                        @Value("${aws.cloudfront.domain}") String cloudFrontDomain,
                        @Value("${aws.cloudfront.ttl}") Integer ttl,
                        @Value("${aws.s3.source-bucket}") String bucket) {
        this.videoRepository = videoRepository;
        this.sessionRepository = sessionRepository;
        this.presignedMultipartProcessor = presignedMultipartProcessor;
        this.objectStorageVerifier = objectStorageVerifier;
        this.eventPublisher = eventPublisher;
        this.signedCookieProcessor = signedCookieProcessor;

        this.CLOUD_FRONT_DOMAIN = cloudFrontDomain;
        this.COOKIE_TTL_SECONDS = ttl;
        this.BUCKET = bucket;
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

        MultipartInitResult result = presignedMultipartProcessor.initMultipart(videoId, BUCKET, sourceKey, contentType, sizeBytes);


        OffsetDateTime expiresAt = OffsetDateTime.ofInstant(
                Instant.ofEpochSecond(result.expiresAtEpochSeconds()),
                ZoneOffset.UTC
        );
        VideoUploadSession session = new VideoUploadSession(
                IdGenerator.generate(),
                videoId,
                BUCKET,
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

        presignedMultipartProcessor.completeMultipart(BUCKET, v.getSourceKey(), uploadId, completedParts);

        objectStorageVerifier.verifyExistsAndSize(BUCKET, v.getSourceKey(), sizeBytes);

        session.markCompleted();
        v.setProcessingStatus(ProcessingStatus.UPLOADED);

        // todo: kafka uploaded 메시지 발송
        eventPublisher.publishEvent(new VideoTranscodeRequestedEvent(videoId));
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

        presignedMultipartProcessor.abortMultipart(BUCKET, v.getSourceKey(), uploadId);
        session.markAborted();
    }

    public PlayResult play(Long videoId) {
        Video v = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("video not found"));

        if (v.getProcessingStatus() != ProcessingStatus.READY) {
            throw new IllegalStateException("비디오가 준비중입니다.");
        }

        if (v.getVisibility() != Visibility.PUBLIC) {
            throw new IllegalStateException("비디오가 비공개 상태입니다.");
        }

        String masterPath = v.getHlsBaseKey() + "master.m3u8"; // key
        String cdnMasterUrl = "https://" + CLOUD_FRONT_DOMAIN + "/" + masterPath;

//        String pathPrefix = "/" + v.getHlsBaseKey();
        String pathPrefix = "videos/" + v.getId() + "/outputs/"; // 다른 경로도 볼 수 있게 하려면 상위 경로로 수정
        long exp = Instant.now().getEpochSecond() + COOKIE_TTL_SECONDS;

        Map<String, String> values = signedCookieProcessor.createSignedCookies(pathPrefix, Duration.ofSeconds(COOKIE_TTL_SECONDS));

        Map<String, ResponseCookie> cookies = signedCookieProcessor.toSetCookieHeaders(
                values,
                ".asinna.store",  // 반드시 상위 도메인
                "/videos/" + v.getId() + "/",                // 또는 "/videos/"+videoId+"/"
                Duration.ofSeconds(COOKIE_TTL_SECONDS),
                true,
                true
        );

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, cookies.get("CloudFront-Policy").toString());
        headers.add(HttpHeaders.SET_COOKIE, cookies.get("CloudFront-Signature").toString());
        headers.add(HttpHeaders.SET_COOKIE, cookies.get("CloudFront-Key-Pair-Id").toString());

        return new PlayResult(headers, cdnMasterUrl, exp);
    }
}
