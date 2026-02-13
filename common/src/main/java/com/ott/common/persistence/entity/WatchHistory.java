package com.ott.common.persistence.entity;

import com.ott.common.persistence.base.BaseEntity;
import com.ott.common.util.IdGenerator;
import jakarta.persistence.*;
import lombok.Getter;



@Entity
@Getter
@Table(
        name = "watch_history",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "video_metadata_id"})
        }
)
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

    protected WatchHistory() {}

    public WatchHistory(User user, VideoMetadata videoMetadata, Integer lastPosition) {
        this.id = IdGenerator.generate();
        this.user = user;
        this.videoMetadata = videoMetadata;
        this.lastPosition = lastPosition;
        this.completed = false;
        this.deleted = false;
    }
}