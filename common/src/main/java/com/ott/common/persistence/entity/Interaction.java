package com.ott.common.persistence.entity;

import com.ott.common.persistence.enums.InteractionType;
import com.ott.common.util.IdGenerator;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "interaction")
public class Interaction {

    @Id
    @Column(name = "interaction_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "video_metadata_id", nullable = false)
    private VideoMetadata videoMetadata;

    @Enumerated(EnumType.STRING)
    @Column(name = "interaction_type", nullable = false)
    private InteractionType interactionType;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public Interaction(User user, VideoMetadata videoMetadata, InteractionType type) {
        this.user = user;
        this.videoMetadata = videoMetadata;
        this.interactionType = type;
        this.createdAt = OffsetDateTime.now();
    }

    @PrePersist
    private void prePersist() {
        if (id == null) id = IdGenerator.generate();
        if (interactionType == null) interactionType = InteractionType.LIKE;
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }

    public void changeType(InteractionType type) {
        this.interactionType = type;
    }
}