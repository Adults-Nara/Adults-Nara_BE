package com.ott.core.modules.search.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ott.core.modules.search.document.VideoDocument;

import java.util.List;

public record VideoFeedResponseDto(
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Long videoId,
        Long userId,
        String title,
        String description,
        String thumbnailUrl,
        Integer duration,
        List<String> tags,
        int viewCount,
        int likeCount,
        String uploadDate
) {
    public static VideoFeedResponseDto from(VideoDocument doc){
        return new VideoFeedResponseDto(
                doc.getVideoId(),
                doc.getUserId(),
                doc.getTitle(),
                doc.getDescription(),
                doc.getThumbnailUrl(),
                doc.getDuration(),
                doc.getTags(),
                doc.getViewCount(),
                doc.getLikeCount(),
                doc.getCreatedAt() != null ? doc.getCreatedAt().toString() : null
        );
    }
}
