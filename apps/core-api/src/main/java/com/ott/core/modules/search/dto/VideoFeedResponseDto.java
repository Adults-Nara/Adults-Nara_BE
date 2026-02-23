package com.ott.core.modules.search.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ott.core.modules.search.document.VideoDocument;

import java.util.List;

public record VideoFeedResponseDto(
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Long videoMetadataId,
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
