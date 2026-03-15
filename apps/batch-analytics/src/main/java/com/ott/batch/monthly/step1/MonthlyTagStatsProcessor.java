package com.ott.batch.monthly.step1;

import com.ott.batch.monthly.dto.MonthlyReportDto;
import com.ott.batch.monthly.dto.TagStatDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

@Slf4j
@Component
public class MonthlyTagStatsProcessor implements ItemProcessor<TagStatDto, MonthlyReportDto> {

    private Integer statsYear;
    private Integer statsMonth;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        String yearMonth = stepExecution.getJobParameters().getString("yearMonth");
        YearMonth ym = YearMonth.parse(yearMonth);
        
        this.statsYear = ym.getYear();
        this.statsMonth = ym.getMonthValue();
        
        log.info("[MonthlyTagStatsProcessor] statsYear={}, statsMonth={}", statsYear, statsMonth);
    }

    @Override
    public MonthlyReportDto process(TagStatDto item) {
        return MonthlyReportDto.builder()
                .userId(item.getUserId())
                .statsYear(statsYear)
                .statsMonth(statsMonth)
                .tagId(item.getTagId())
                .tagName(item.getTagName())
                .totalWatchSeconds(item.getTotalWatchSeconds())
                .watchCount(item.getWatchCount())
                .build();
    }
}
