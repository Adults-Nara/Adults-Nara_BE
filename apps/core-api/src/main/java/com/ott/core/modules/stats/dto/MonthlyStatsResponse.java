package com.ott.core.modules.stats.dto;

import com.ott.core.modules.usertag.dto.TagWatchStatsResponse;
import java.util.List;

public record MonthlyStatsResponse(
        Integer statsYear,
        Integer statsMonth,
        List<TagWatchStatsResponse> tagWatchStats,
        NavigationInfo navigation
) {
    public record NavigationInfo(
            Integer prevYear,
            Integer prevMonth,
            Integer nextYear,
            Integer nextMonth,
            Boolean hasPrev,
            Boolean hasNext
    ) {}
}