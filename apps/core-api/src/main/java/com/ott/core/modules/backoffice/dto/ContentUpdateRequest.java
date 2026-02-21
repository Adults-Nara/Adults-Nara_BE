package com.ott.core.modules.backoffice.dto;

import com.ott.common.persistence.enums.Visibility;

import java.util.List;

public record ContentUpdateRequest(
        String title,
        String description,
        Visibility visibility,
        String thumbnailUrl,
        List<Long> tagIds
) {
}
