package com.ott.batch.monthly.dto;

import com.ott.common.persistence.entity.MonthlyWatchReport;
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

    public MonthlyWatchReport toEntity() {
        return MonthlyWatchReport.builder()
                .userId(userId)
                .statsYear(statsYear)
                .statsMonth(statsMonth)
                .tagId(tagId)
                .tagName(tagName)
                .totalWatchSeconds(totalWatchSeconds)
                .watchCount(watchCount)
                .build();
    }
}
