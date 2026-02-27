package com.ott.core.modules.bookmark.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
@AllArgsConstructor
public class BookmarkListResponse {
    private String videoId;
    private String title;
    private String thumbnailUrl;
    private int viewCount;
    private String uploaderName;
    private double watchProgressPercent;
    private Integer duration;
    private OffsetDateTime uploadDate;
}
