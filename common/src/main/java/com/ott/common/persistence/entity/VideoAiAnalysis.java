package com.ott.common.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

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

    @Column(name = "embedding", columnDefinition = "JSON")
    private String embeddingJson;

    @Column(name = "analyzed_at", nullable = false)
    private OffsetDateTime createdAt;

    @Builder
    private VideoAiAnalysis(Long id, String summary,
            String subtitleUrl, String embeddingJson,
            OffsetDateTime createdAt) {
        this.id = id;
        this.summary = summary;
        this.subtitleUrl = subtitleUrl;
        this.embeddingJson = embeddingJson;
        this.createdAt = createdAt;
    }
}
