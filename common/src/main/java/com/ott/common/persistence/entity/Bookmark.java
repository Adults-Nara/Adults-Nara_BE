package com.ott.common.persistence.entity;

import com.ott.common.persistence.base.BaseEntity;
import com.ott.common.util.IdGenerator;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "bookmark",uniqueConstraints = {
        @UniqueConstraint(name = "uk_bookmark_user_video", columnNames = {"user_id", "video_metadata_id"})
})
public class Bookmark extends BaseEntity {

    @Id
    @Column(name = "bookmark_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "video_metadata_id", nullable = false)
    private VideoMetadata videoMetadata;


    public Bookmark(User user, VideoMetadata videoMetadata) {
        this.user = user;
        this.videoMetadata = videoMetadata;
    }

    @PrePersist
    private void prePersist() {
        if (id == null) id = IdGenerator.generate();
    }
}