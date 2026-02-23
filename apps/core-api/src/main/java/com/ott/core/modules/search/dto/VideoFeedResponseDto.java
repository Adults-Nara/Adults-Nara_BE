package com.ott.core.modules.search.dto;

import com.ott.core.modules.search.document.VideoDocument;

import java.util.List;

public record VideoFeedResponseDto(
        String videoMetadataId,
        String title,
        List<String> tags,
        int viewCount,
        String uploadDate
) {
    public static VideoFeedResponseDto from(VideoDocument doc){
        return new VideoFeedResponseDto(
                doc.getId(),
                doc.getTitle(),
                doc.getTags(),
                doc.getViewCount(),
                doc.getCreatedAt()
        );
    }
}
