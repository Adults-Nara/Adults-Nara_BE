package com.ott.core.modules.watch.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
@AllArgsConstructor
public class WatchHistoryItemResponse {
    private String videoId;
    private String title;
    private String thumbnailUrl;
    private int viewCount;
    private String uploaderName;
    private double watchProgressPercent;
    private OffsetDateTime watchedAt;
    private Integer duration;
}
