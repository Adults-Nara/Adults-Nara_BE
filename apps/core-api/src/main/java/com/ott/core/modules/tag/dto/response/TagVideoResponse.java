package com.ott.core.modules.tag.dto.response;

import java.time.OffsetDateTime;

public record TagVideoResponse(
        String videoId,
        String title,
        String thumbnailUrl,
        int viewCount,
        String uploaderName,
        double watchProgressPercent,
        OffsetDateTime uploadedAt,
        Integer duration
) {
}
