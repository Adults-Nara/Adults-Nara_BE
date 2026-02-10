package com.ott.common.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class VideoTagId implements Serializable {

    @Column(name = "tag_id", nullable = false)
    private Long tagId;

    @Column(name = "video_metadata_id", nullable = false)
    private Long videoMetadataId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VideoTagId)) return false;
        VideoTagId that = (VideoTagId) o;
        return Objects.equals(tagId, that.tagId) &&
                Objects.equals(videoMetadataId, that.videoMetadataId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tagId, videoMetadataId);
    }
}