package com.ott.core.modules.backoffice.dto;

import com.ott.common.persistence.enums.Visibility;

import java.util.List;

public record ContentStatusUpdateRequest(
        List<Long> videoIds,
        Visibility visibility
) {
}
