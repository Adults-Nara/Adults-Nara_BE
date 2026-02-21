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
@Table(name = "comments")
public class Comment extends BaseEntity {

    @Id
    @Column(name = "comments_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "video_metadata_id", nullable = false)
    private VideoMetadata videoMetadata;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String text;

    @Column(nullable = false)
    private boolean deleted;

    public Comment(VideoMetadata videoMetadata, User user, String text) {
        this.videoMetadata = videoMetadata;
        this.user = user;
        this.text = text;
        this.deleted = false;
    }

    @PrePersist
    private void prePersist() {
        if (id == null) id = IdGenerator.generate();
    }

    public void edit(String text) {
        this.text = text;
    }

    public void softDelete() {
        this.deleted = true;
    }
}