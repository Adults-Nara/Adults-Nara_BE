package com.ott.core.modules.backoffice.dto;

import com.ott.common.persistence.enums.Visibility;

import java.time.OffsetDateTime;
import java.util.List;

public record ContentDetailResponse(
        String videoMetadataId,
        String title,
        String description,
        String thumbnailUrl,
        Visibility visibility,
        List<String> tagIds,
        OffsetDateTime createdAt
) {
}
