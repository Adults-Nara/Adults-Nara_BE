package com.ott.common.persistence.entity;

import com.ott.common.persistence.base.BaseEntity;
import com.ott.common.util.IdGenerator;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.OffsetDateTime;


@Entity
@Getter
@Table(name = "watch_history")
public class WatchHistory extends BaseEntity {

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
    private boolean deleted;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    protected WatchHistory() {}

    public WatchHistory(User user, VideoMetadata videoMetadata) {
        this.id = IdGenerator.generate();
        this.user = user;
        this.videoMetadata = videoMetadata;
        this.lastPosition = 0;
        this.completed = false;
        this.deleted = false;
    }
}