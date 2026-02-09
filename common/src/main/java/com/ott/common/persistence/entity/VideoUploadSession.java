package com.ott.common.persistence.entity;

import com.ott.common.persistence.enums.UploadSessionStatus;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "video_upload_sessions",
        indexes = {
            @Index(name = "idx_upload_session_video_status", columnList = "videoId,status"),
            @Index(name = "idx_upload_session_expires", columnList = "status,expiresAt")
        })
public class VideoUploadSession {

    @Id
    private Long id;

    private Long videoId;

    private String bucket;

    private String objectKey;

    private String s3UploadId;

    @Enumerated(EnumType.STRING)
    private UploadSessionStatus status;

    private long partSizeBytes;

    private long expectedSizeBytes;

    private OffsetDateTime expiresAt;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    protected VideoUploadSession() { }

    public VideoUploadSession(Long id,
                              Long videoId,
                              String bucket,
                              String objectKey,
                              String s3UploadId,
                              long partSizeBytes,
                              long expectedSizeBytes,
                              OffsetDateTime expiresAt) {
        this.id = id;
        this.videoId = videoId;
        this.bucket = bucket;
        this.objectKey = objectKey;
        this.s3UploadId = s3UploadId;
        this.status = UploadSessionStatus.UPLOADING;
        this.partSizeBytes = partSizeBytes;
        this.expectedSizeBytes = expectedSizeBytes;
        this.expiresAt = expiresAt;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public void markCompleted() {
        this.status = UploadSessionStatus.COMPLETED;
        this.updatedAt = OffsetDateTime.now();
    }

    public void markAborted() {
        this.status = UploadSessionStatus.ABORTED;
        this.updatedAt = OffsetDateTime.now();
    }

    public void markExpired() {
        this.status = UploadSessionStatus.EXPIRED;
        this.updatedAt = OffsetDateTime.now();
    }

    public boolean isActive() {
        return this.status == UploadSessionStatus.UPLOADING;
    }

    public boolean isExpiredNow() {
        return OffsetDateTime.now().isAfter(this.expiresAt);
    }

    public Long getId() {
        return id;
    }

    public Long getVideoId() {
        return videoId;
    }

    public String getBucket() {
        return bucket;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public String getS3UploadId() {
        return s3UploadId;
    }

    public UploadSessionStatus getStatus() {
        return status;
    }

    public long getPartSizeBytes() {
        return partSizeBytes;
    }

    public long getExpectedSizeBytes() {
        return expectedSizeBytes;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
