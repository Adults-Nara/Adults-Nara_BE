package com.ott.core.modules.backoffice.dto;

import com.ott.common.persistence.enums.Visibility;

import java.time.OffsetDateTime;

public record AdminContentResponse(
        String videoId,
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
