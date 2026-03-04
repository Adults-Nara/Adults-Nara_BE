package com.ott.batch.monthly.step1;

import com.ott.batch.monthly.dto.TagStatDto;
import com.ott.batch.monthly.dto.UserTagWatchRaw;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * Step1 Processor: UserTagWatchRaw → TagStatDto 변환.
 *
 * stats_date는 전월 1일로 고정 (월 단위 집계를 하나의 날짜로 표현).
 */
@Slf4j
@StepScope
@Component("tagStatItemProcessor")
public class TagStatProcessor implements ItemProcessor<UserTagWatchRaw, TagStatDto> {

    private final LocalDate statsDate;

    public TagStatProcessor(@Value("#{jobParameters['yearMonth']}") String yearMonthStr) {
        YearMonth ym = YearMonth.parse(yearMonthStr, DateTimeFormatter.ofPattern("yyyy-MM"));
        this.statsDate = ym.atDay(1);
    }

    @Override
    public TagStatDto process(UserTagWatchRaw raw) {
        return TagStatDto.builder()
                .userId(raw.getUserId())
                .tagId(raw.getTagId())
                .tagName(raw.getTagName())
                .statsDate(statsDate)
                .totalViewTime(raw.getTotalViewTime())
                .viewCount(raw.getViewCount())
                .build();
    }
}