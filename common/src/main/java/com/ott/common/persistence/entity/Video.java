package com.ott.common.persistence.entity;

import com.ott.common.persistence.enums.ProcessingStatus;
import com.ott.common.persistence.enums.Visibility;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "videos")
public class Video {
    @Id
    private Long id;

    @Enumerated(EnumType.STRING)
    private ProcessingStatus processingStatus;

    @Enumerated(EnumType.STRING)
    private Visibility visibility;

    private String sourceKey; // videos/{videoId}/source/source.mp4

    private Integer currentEncodeVersion; // 1, 2, 3...

    private String hlsBaseKey; // videos/{videoId}/outputs/hls/v{version}/

    private OffsetDateTime createdAt;

    private OffsetDateTime publishedAt;

    private OffsetDateTime updatedAt;

    protected Video() {}

    public Video(Long id, String sourceKey) {
        this.id = id;
        this.sourceKey = sourceKey;
        this.processingStatus = ProcessingStatus.UPLOADING;
        this.visibility = Visibility.PRIVATE;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public void markReady(int version) {
        this.processingStatus = ProcessingStatus.READY;
        this.currentEncodeVersion = version;
        this.hlsBaseKey = "videos/" + id + "/outputs/hls/v" + version + "/";
        this.updatedAt = OffsetDateTime.now();
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
        if (visibility == Visibility.PUBLIC) {
            this.publishedAt = OffsetDateTime.now();
        }
        this.updatedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public ProcessingStatus getProcessingStatus() {
        return processingStatus;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public String getSourceKey() {
        return sourceKey;
    }

    public Integer getCurrentEncodeVersion() {
        return currentEncodeVersion;
    }

    public String getHlsBaseKey() {
        return hlsBaseKey;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setProcessingStatus(ProcessingStatus processingStatus) {
        this.processingStatus = processingStatus;
        updatedAt = OffsetDateTime.now();
    }
}
