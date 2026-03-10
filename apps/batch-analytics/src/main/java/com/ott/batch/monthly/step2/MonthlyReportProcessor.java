package com.ott.batch.monthly.step2;

import com.ott.batch.monthly.dto.MonthlyReportDto;
import com.ott.batch.repository.TagStatsRepository;
import com.ott.batch.repository.WatchHistoryRepository;
import com.ott.common.persistence.entity.Tag;
import com.ott.common.persistence.entity.TagStats;
import com.ott.common.persistence.entity.WatchHistory;
import com.ott.common.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 사용자별 월간 리포트 생성 Processor
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyReportProcessor implements ItemProcessor<Long, MonthlyReportDto> {

    private final TagStatsRepository tagStatsRepository;
    private final WatchHistoryRepository watchHistoryRepository;

    private OffsetDateTime rangeFrom;
    private OffsetDateTime rangeTo;
    private LocalDate startDate;
    private LocalDate endDate;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        String yearMonth = stepExecution.getJobParameters().getString("yearMonth");
        String rangeFromStr = stepExecution.getJobParameters().getString("rangeFrom");
        String rangeToStr = stepExecution.getJobParameters().getString("rangeTo");

        this.rangeFrom = OffsetDateTime.parse(rangeFromStr);
        this.rangeTo = OffsetDateTime.parse(rangeToStr);

        LocalDate firstDayOfMonth = LocalDate.parse(yearMonth + "-01");
        this.startDate = firstDayOfMonth;
        this.endDate = firstDayOfMonth.withDayOfMonth(firstDayOfMonth.lengthOfMonth());

        log.debug("[MonthlyReportProcessor] 초기화: yearMonth={}, 기간: {} ~ {}",
                yearMonth, startDate, endDate);
    }

    @Override
    public MonthlyReportDto process(Long userId) {
        // N+1 해결: Tag를 JOIN FETCH로 한 번에 로딩
        List<TagStats> tagStatsList = tagStatsRepository.findByUserIdAndStatsDateBetweenWithTag(
                userId, startDate, endDate
        );

        if (tagStatsList.isEmpty()) {
            log.warn("[MonthlyReportProcessor] userId={}의 태그 통계가 없음", userId);
            return null;  // skip
        }

        // 시간대별 집계를 위해 WatchHistory 조회
        List<WatchHistory> watchHistories = watchHistoryRepository.findByUserIdAndCreatedAtBetween(
                userId, rangeFrom, rangeTo
        );

        if (watchHistories.isEmpty()) {
            log.warn("[MonthlyReportProcessor] userId={}의 시청 기록이 없음", userId);
            return null;
        }

        // 전체 시청 시간
        long totalWatchSeconds = watchHistories.stream()
                .mapToLong(WatchHistory::getLastPosition)
                .sum();

        // 완주 수
        int completedCount = (int) watchHistories.stream()
                .filter(WatchHistory::isCompleted)
                .count();

        // 완주율 계산 (빈 리스트는 이미 체크됨)
        BigDecimal completionRate = BigDecimal.valueOf(completedCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(watchHistories.size()), 2, RoundingMode.HALF_UP);

        // 시간대별 집계 (Stream으로 개선)
        Map<String, Long> timeSlotCounts = watchHistories.stream()
                .collect(Collectors.groupingBy(
                        wh -> getTimeSlot(wh.getCreatedAt().getHour()),
                        Collectors.counting()
                ));

        // 주 시청 시간대 (기본값 NONE)
        String peakTimeSlot = timeSlotCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> entry.getKey())
                .orElse("NONE");

        // 최장 시청 세션
        int longestSessionSeconds = watchHistories.stream()
                .mapToInt(WatchHistory::getLastPosition)
                .max()
                .orElse(0);

        // 가장 많이 본 태그 (월 전체 합산)
        String mostWatchedTagName = tagStatsList.stream()
                .collect(Collectors.groupingBy(
                        TagStats::getTag,
                        Collectors.summingLong(TagStats::getTotalViewTime)
                ))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> entry.getKey().getTagName())
                .orElse("없음");

        // 다양성 점수 (고유 부모 태그 수 * 20, 최대 100점)
        int diversityScore = (int) tagStatsList.stream()
                .map(ts -> ts.getTag().getParent())
                .filter(Objects::nonNull)
                .map(Tag::getId)
                .distinct()
                .count() * 20;
        diversityScore = Math.min(100, diversityScore);

        log.debug("[MonthlyReportProcessor] userId={}, watchCount={}, completed={}, diversity={}",
                userId, watchHistories.size(), completedCount, diversityScore);

        return MonthlyReportDto.builder()
                .id(IdGenerator.generate())
                .userId(userId)
                .reportYearMonth(startDate.toString().substring(0, 7))
                .totalWatchSeconds(totalWatchSeconds)
                .totalWatchCount(watchHistories.size())
                .completedCount(completedCount)
                .completionRate(completionRate)
                .dawnCount(timeSlotCounts.getOrDefault("DAWN", 0L).intValue())
                .morningCount(timeSlotCounts.getOrDefault("MORNING", 0L).intValue())
                .afternoonCount(timeSlotCounts.getOrDefault("AFTERNOON", 0L).intValue())
                .eveningCount(timeSlotCounts.getOrDefault("EVENING", 0L).intValue())
                .nightCount(timeSlotCounts.getOrDefault("NIGHT", 0L).intValue())
                .peakTimeSlot(peakTimeSlot)
                .longestSessionSeconds(longestSessionSeconds)
                .mostWatchedTagName(mostWatchedTagName)
                .diversityScore(diversityScore)
                .build();
    }

    private String getTimeSlot(int hour) {
        if (hour >= 0 && hour <= 5) return "DAWN";      // 새벽 (0-5시)
        if (hour >= 6 && hour <= 11) return "MORNING";  // 오전 (6-11시)
        if (hour >= 12 && hour <= 17) return "AFTERNOON"; // 오후 (12-17시)
        if (hour >= 18 && hour <= 21) return "EVENING";  // 저녁 (18-21시)
        return "NIGHT";  // 밤 (22-23시)
    }
}