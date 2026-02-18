package com.ott.core.modules.video.dto.backoffice;

import com.ott.common.persistence.enums.Visibility;

import java.time.OffsetDateTime;

public record UploaderContentResponse(
        Long contentId,
        String thumbnailUrl,
        String title,
        String description,
        String otherVideoUrl,
        int viewCount,
        int likeCount,
        int dislikeCount,
        int commentCount,
        Visibility visibility,
        OffsetDateTime createdAt
) {
}
