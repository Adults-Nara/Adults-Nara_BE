package com.ott.core.modules.watch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchHistoryDto {
    private Long userId;
    private Long videoId;
    private Integer lastPosition;
    private Integer duration;
}
