package com.ott.core.modules.watch.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class WatchHistoryResponse {
    private String videoId;
    private Integer lastPosition;
    private Integer duration;
}
