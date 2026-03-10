package com.ott.common.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "video_ai_analysis")
public class VideoAiAnalysis {
    @Id
    @Column(name = "video_id")
    private Long id;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "subtitle_url", length = 1000)
    private String subtitleUrl;

    @Column(name = "embedding", columnDefinition = "vector(384)")
    private float[] embedding;

    @Column(name = "analyzed_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }

    @Builder
    private VideoAiAnalysis(Long id, String summary,
            String subtitleUrl, float[] embedding) {
        this.id = id;
        this.summary = summary;
        this.subtitleUrl = subtitleUrl;
        this.embedding = embedding;
    }
}
