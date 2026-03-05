package com.ott.batch.monthly.step2;

import com.ott.batch.monthly.dto.MonthlyReportDto;
import com.ott.batch.repository.TagStatsRepository;
import com.ott.common.persistence.entity.TagStats;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Step 2: 월간 리포트 Processor
 */
@Slf4j
@Component
public class MonthlyReportProcessor implements ItemProcessor<Long, MonthlyReportDto>, StepExecutionListener {

    private final TagStatsRepository tagStatsRepository;
    private String yearMonth;
    private LocalDate startDate;
    private LocalDate endDate;

    public MonthlyReportProcessor(TagStatsRepository tagStatsRepository) {
        this.tagStatsRepository = tagStatsRepository;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        // Job Parameters에서 yearMonth 가져오기
        this.yearMonth = stepExecution.getJobParameters().getString("yearMonth");

        // yearMonth를 LocalDate로 변환
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        LocalDate firstDayOfMonth = LocalDate.parse(yearMonth + "-01");
        this.startDate = firstDayOfMonth;
        this.endDate = firstDayOfMonth.withDayOfMonth(firstDayOfMonth.lengthOfMonth());

        log.debug("[MonthlyReportProcessor] 초기화: yearMonth={}, 기간: {} ~ {}",
                yearMonth, startDate, endDate);
    }

    @Override
    public MonthlyReportDto process(Long userId) {
        // 해당 사용자의 월간 태그 통계 조회
        List<TagStats> tagStatsList = tagStatsRepository.findByUserIdAndStatsDateBetween(
                userId, startDate, endDate
        );

        if (tagStatsList.isEmpty()) {
            log.warn("[MonthlyReportProcessor] userId={}의 태그 통계가 없음", userId);
            return null;  // skip
        }

        // 집계 계산 (Integer → long 변환)
        long totalWatchSeconds = tagStatsList.stream()
                .mapToLong(ts -> ts.getTotalViewTime().longValue())  // Integer → long
                .sum();

        int totalWatchCount = tagStatsList.stream()
                .mapToInt(TagStats::getViewCount)
                .sum();

        int completedCount = tagStatsList.stream()
                .mapToInt(TagStats::getCompletedCount)
                .sum();

        double completionRate = totalWatchCount > 0
                ? Math.round((double) completedCount / totalWatchCount * 10000.0) / 100.0
                : 0.0;

        // 시간대별 집계는 WatchHistory에서 직접 계산 필요
        // (여기서는 간단히 0으로 설정 - 실제로는 WatchHistory 조회 필요)
        int dawnCount = 0;
        int morningCount = 0;
        int afternoonCount = 0;
        int eveningCount = 0;
        int nightCount = 0;
        String peakTimeSlot = "UNKNOWN";

        // 최장 시청 시간 (태그별 최대값 중 최대)
        int longestSessionSeconds = tagStatsList.stream()
                .mapToInt(TagStats::getTotalViewTime)  // Integer
                .max()
                .orElse(0);

        // 최애 태그 (총 시청 시간이 가장 긴 태그)
        String mostWatchedTagName = tagStatsList.stream()
                .max(Comparator.comparing(TagStats::getTotalViewTime))
                .map(ts -> {
                    // Tag 객체를 통해 접근
                    if (ts.getTag() != null) {
                        return ts.getTag().getTagName();
                    }
                    return "UNKNOWN";
                })
                .orElse("NONE");

        // 다양성 점수 (고유 태그 수 * 20, 최대 100)
        long uniqueTagCount = tagStatsList.stream()
                .map(TagStats::getTag)        // Tag 객체 가져오기
                .filter(Objects::nonNull)
                .map(tag -> tag.getId())      // Tag의 ID
                .distinct()
                .count();

        int diversityScore = Math.min((int) (uniqueTagCount * 20), 100);

        log.debug("[MonthlyReportProcessor] userId={}, watchCount={}, completed={}, diversity={}",
                userId, totalWatchCount, completedCount, diversityScore);

        return MonthlyReportDto.builder()
                .userId(userId)
                .reportYearMonth(yearMonth)
                .totalWatchSeconds(totalWatchSeconds)
                .totalWatchCount(totalWatchCount)
                .completedCount(completedCount)
                .completionRate(completionRate)
                .dawnCount(dawnCount)
                .morningCount(morningCount)
                .afternoonCount(afternoonCount)
                .eveningCount(eveningCount)
                .nightCount(nightCount)
                .peakTimeSlot(peakTimeSlot)
                .longestSessionSeconds(longestSessionSeconds)
                .mostWatchedTagName(mostWatchedTagName)
                .diversityScore(diversityScore)
                .build();
    }
}