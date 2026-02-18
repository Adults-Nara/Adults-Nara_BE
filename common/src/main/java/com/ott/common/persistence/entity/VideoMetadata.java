package com.ott.common.persistence.entity;

import com.ott.common.persistence.base.BaseEntity;
import com.ott.common.persistence.enums.VideoType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "video_metadata")
public class VideoMetadata extends BaseEntity {
    @Id
    @Column(name = "video_metadata_id")
    private Long id;

    private Long videoId;

    private Long userId;

    @Column(length = 255)
    private String title;

    @Column(length = 4000)
    private String description;

    @Column(length = 1000)
    private String thumbnailUrl;

    private int viewCount;

    private int likeCount;

    private int dislikeCount;

    private int bookmarkCount;

    private int commentCount;

    private Integer duration;

    private Long otherVideoMetadataId;

    private String otherVideoUrl;

    private VideoType videoType;

    private boolean deleted;

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public void setOtherVideoUrl(String otherVideoUrl) {
        this.otherVideoUrl = otherVideoUrl;
    }

    public void setVideoType(VideoType videoType) {
        this.videoType = videoType;
    }
}
