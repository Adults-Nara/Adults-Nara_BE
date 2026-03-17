package com.ott.core.modules.stats.dto;

public record TagStatsDto(
        Long tagId,
        String tagName,
        Long totalWatchSeconds,
        Integer watchCount
) {}