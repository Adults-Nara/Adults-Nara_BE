package com.ott.batch.monthly.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyReportDto {
    private Long userId;
    private Integer statsYear;
    private Integer statsMonth;
    private Long tagId;
    private String tagName;
    private Long totalWatchSeconds;
    private Integer watchCount;
}
