package com.ott.core.modules.search.dto;

import com.ott.core.modules.search.document.VideoDocument;
import lombok.Builder;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Builder
public record VideoSearchResponse(
        Long videoId,
        String title,
        String thumbnailUrl,
        int viewCount,
        int duration,
        OffsetDateTime createdAt
) {
    public static VideoSearchResponse from(VideoDocument document) {
        return VideoSearchResponse.builder()
                .videoId(document.getVideoId())
                .title(document.getTitle())
                .thumbnailUrl(document.getThumbnailUrl())
                .viewCount(document.getViewCount())
                .duration(document.getDuration() != null ? document.getDuration() : 0)
                .createdAt(document.getCreatedAt())
                .build();
    }
}