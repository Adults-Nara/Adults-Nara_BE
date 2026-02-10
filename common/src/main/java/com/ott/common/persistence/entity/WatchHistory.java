package com.ott.common.persistence.entity;

import com.ott.common.util.IdGenerator;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.OffsetDateTime;

@Entity
@Getter
@Table(name = "watch_history")
public class WatchHistory {

    @Id
    @Column(name = "watch_history_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "video_metadata_id", nullable = false)
    private VideoMetadata videoMetadata;

    @Column(name = "last_position", nullable = false)
    private Integer lastPosition;

    @Column(nullable = false)
    private boolean completed;

    @Column(nullable = false)
    private boolean disliked;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    protected WatchHistory() {}

    public WatchHistory(User user, VideoMetadata videoMetadata) {
        this.user = user;
        this.videoMetadata = videoMetadata;
        this.lastPosition = 0;
        this.completed = false;
        this.disliked = false;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    @PrePersist
    private void prePersist() {
        if (id == null) id = IdGenerator.generate();
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (updatedAt == null) updatedAt = OffsetDateTime.now();
    }

    public void updatePosition(int seconds) {
        this.lastPosition = seconds;
        this.updatedAt = OffsetDateTime.now();
    }

    public void markCompleted() {
        this.completed = true;
        this.updatedAt = OffsetDateTime.now();
    }

    public void setDisliked(boolean disliked) {
        this.disliked = disliked;
        this.updatedAt = OffsetDateTime.now();
    }
}