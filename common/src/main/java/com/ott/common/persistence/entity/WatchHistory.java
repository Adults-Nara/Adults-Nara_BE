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

    // 완주로 판단할 비율 (90%)
    private static final double COMPLETION_THRESHOLD = 0.9;

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

    public static boolean isVideoCompleted(Integer lastPosition, Integer duration) {

        if (duration == null || duration <= 0) { // 방어 로직: 영상 길이가 없거나 0이면 완주 불가
            return false;
        }
        if (lastPosition == null) { // 방어 로직: 시청 위치가 없으면 미완주
            return false;
        }
        return (double) lastPosition / duration >= COMPLETION_THRESHOLD; // 90% 이상 시청 시 true 반환
    }
}