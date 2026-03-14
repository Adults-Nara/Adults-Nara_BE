package com.ott.batch.monthly.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TagStatDto {
    private Long userId;
    private Long tagId;
    private String tagName;
    private Long totalWatchSeconds;
    private Integer watchCount;
}
