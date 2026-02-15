package com.ott.common.persistence.entity;

import com.ott.common.persistence.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.OffsetDateTime;

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

    @Column(length = 255)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(length = 1000)
    private String thumbnailUrl;

    private int viewCount;

    private int likeCount;

    private int bookmarkCount;

    private Integer duration;

    private Long otherVideoMetadataId;

    private boolean deleted;

    public void setDuration(Integer duration) {
        this.duration = duration;
    }
}
