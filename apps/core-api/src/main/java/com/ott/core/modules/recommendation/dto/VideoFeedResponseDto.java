package com.ott.core.modules.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ott.common.persistence.enums.VideoType;
import com.ott.core.modules.search.document.VideoDocument;

import java.util.List;

public record VideoFeedResponseDto(
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Long videoId,
        Long userId,
        String uploaderNickname,
        String uploaderProfileImageUrl,
        String title,
        String description,
        String thumbnailUrl,
        Integer duration,
        List<String> tags,
        int viewCount,
        int likeCount,
        String uploadDate,
        VideoType videoType,
        Integer watchProgress
) {
    public static VideoFeedResponseDto of(VideoDocument doc, String nickname, String profileUrl, Integer watchProgress){
        return new VideoFeedResponseDto(
                doc.getVideoId(),
                doc.getUserId(),
                nickname,
                profileUrl,
                doc.getTitle(),
                doc.getDescription(),
                doc.getThumbnailUrl(),
                doc.getDuration(),
                doc.getTags(),
                doc.getViewCount(),
                doc.getLikeCount(),
                doc.getCreatedAt() != null ? doc.getCreatedAt().toString() : null,
                doc.getVideoType(),
                watchProgress
        );
    }
}
