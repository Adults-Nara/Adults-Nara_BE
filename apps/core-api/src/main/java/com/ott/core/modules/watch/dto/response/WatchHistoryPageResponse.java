package com.ott.core.modules.watch.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class WatchHistoryPageResponse {
    private List<WatchHistoryItemResponse> items;
    private boolean hasMore;
}
