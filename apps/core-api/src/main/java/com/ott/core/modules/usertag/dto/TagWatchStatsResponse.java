package com.ott.core.modules.usertag.dto;

public record TagWatchStatsResponse (
        String tagId,
        String tagName,
        Long totalWatchSeconds
) {
}
