package com.ott.core.modules.video.dto;

import com.ott.common.persistence.enums.Visibility;

import java.time.OffsetDateTime;
import java.util.List;

public record VideoInfoResult(
        String videoId,
        String title,
        String description,
        String thumbnailUrl,
        Visibility visibility,
        List<String> tagIds,
        OffsetDateTime createdAt,
        String otherVideoUrl
) {
}
