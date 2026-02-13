package com.ott.common.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "video_tag",
    uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_video_tag",
                columnNames = {"video_metadata_id", "tag_id"}
        )
    })
public class VideoTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "video_tag_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_metadata_id", nullable = false)
    private VideoMetadata videoMetadata;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    public VideoTag(VideoMetadata videoMetadata, Tag tag) {
        this.videoMetadata = videoMetadata;
        this.tag = tag;
    }
}