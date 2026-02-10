package com.ott.common.persistence.entity;

import com.ott.common.util.IdGenerator;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "bookmark")
public class Bookmark {

    @Id
    @Column(name = "bookmark_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "video_metadata_id", nullable = false)
    private VideoMetadata videoMetadata;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public Bookmark(User user, VideoMetadata videoMetadata) {
        this.user = user;
        this.videoMetadata = videoMetadata;
        this.createdAt = OffsetDateTime.now();
    }

    @PrePersist
    private void prePersist() {
        if (id == null) id = IdGenerator.generate();
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}