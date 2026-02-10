package com.ott.common.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;

import java.time.OffsetDateTime;

@Entity
@Getter
public class VideoMetadata {
    @Id
    private Long id;

    private Long videoId;

    @Column(length = 255)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(length = 1000)
    private String thumbnailUrl;

    private int viewCount;

    private int bookmarkCount;

    private Long otherVideoMetadataId;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    protected VideoMetadata() { }

    public VideoMetadata(Long id, Long videoId, String title) {
        this.id = id;
        this.title = title;
        this.viewCount = 0;
        this.bookmarkCount = 0;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public VideoMetadata(Long id, Long videoId, String title, String description, String thumbnailUrl) {
        this(id, videoId, title);
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
    }
}
