package com.ott.core.modules.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ott.common.persistence.enums.VideoType;
import com.ott.core.modules.search.document.VideoDocument;

import java.util.List;

public record VideoFeedResponseDto(
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Long videoId,
        String thumbnailSrc,
        String title,
        String uploader,
        String uploaderProfileImageUrl,
        int progress,
        Integer duration,
        int views,
        String date,
        Long userId,
        VideoType videoType
) {
    public static VideoFeedResponseDto of(VideoDocument doc, String nickname, String profileUrl, int progress){
        return new VideoFeedResponseDto(
                doc.getVideoId(),
                doc.getThumbnailUrl(),
                doc.getTitle(),
                nickname,
                profileUrl,
                progress,
                doc.getDuration(),
                doc.getViewCount(),
                doc.getCreatedAt() != null ? doc.getCreatedAt().toString() : null, // date
                doc.getUserId(),
                doc.getVideoType()
        );
    }
}
