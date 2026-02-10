package com.ott.common.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "video_tag")
public class VideoTag {

    @EmbeddedId
    private VideoTagId id;

    @MapsId("tagId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    @MapsId("videoMetadataId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "video_metadata_id", nullable = false)
    private VideoMetadata videoMetadata;

    public VideoTag(Tag tag, VideoMetadata videoMetadata) {
        this.tag = tag;
        this.videoMetadata = videoMetadata;
        this.id = new VideoTagId(tag.getId(), videoMetadata.getId());
    }
}