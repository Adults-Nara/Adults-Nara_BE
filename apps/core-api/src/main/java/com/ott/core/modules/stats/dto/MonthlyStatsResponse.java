package com.ott.core.modules.stats.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record MonthlyStatsResponse(
        Integer year,
        Integer month,
        Long userId,
        List<TagStatsDto> tags,
        Boolean hasPrevious,
        Boolean hasNext,
        String previousMonth,
        String nextMonth
) {}