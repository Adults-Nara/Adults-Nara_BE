package com.ott.common.persistence.entity;

import com.ott.common.persistence.base.BaseEntity;
import com.ott.common.persistence.enums.VideoType;
import com.ott.common.util.IdGenerator;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "video_metadata", indexes = {
        @Index(name = "idx_video_metadata_video_id", columnList = "videoId", unique = true)
})
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

    @Builder.Default
    private int viewCount = 0;

    @Builder.Default
    private int likeCount = 0;

    @Builder.Default
    private int dislikeCount = 0;

    @Builder.Default
    private int superLikeCount = 0;

    @Builder.Default
    private int bookmarkCount = 0;

    //광고 여부
    @Builder.Default
    private boolean isAd = false;

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

    public void softDelete() {
        this.deleted = true;
    }

    @PrePersist
    private void prePersist() {
        if (id == null) id = IdGenerator.generate();
    }
}
