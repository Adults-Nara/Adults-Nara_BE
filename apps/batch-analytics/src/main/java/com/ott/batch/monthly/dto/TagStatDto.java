package com.ott.batch.monthly.dto;

import lombok.Builder;

@Builder
public record TagStatDto(
    Long userId,
    Long tagId,
    String tagName,
    Long totalWatchSeconds,
    Integer watchCount
) {}
