package com.ott.batch.monthly.step1;

import com.ott.batch.monthly.dto.TagStatDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;

@Slf4j
@Component
public class TagStatProcessor implements ItemProcessor<TagStatDto, TagStatDto> {

    private LocalDate statsDate;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        String yearMonth = stepExecution.getJobParameters().getString("yearMonth");
        this.statsDate = YearMonth.parse(yearMonth).atDay(1);
        log.debug("[TagStatProcessor] statsDate 설정: {}", statsDate);
    }

    @Override
    public TagStatDto process(TagStatDto item) {
        if (item.getViewCount() == null || item.getViewCount() <= 0) {
            log.warn("[TagStatProcessor] 유효하지 않은 데이터 스킵: {}", item);
            return null;
        }

        // statsDate 설정
        item.setStatsDate(statsDate);

        return item;
    }
}