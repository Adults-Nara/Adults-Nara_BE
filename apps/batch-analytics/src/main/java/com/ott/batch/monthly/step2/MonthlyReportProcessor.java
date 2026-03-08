package com.ott.batch.monthly.step2;

import com.ott.batch.monthly.dto.MonthlyReportDto;
import com.ott.batch.repository.TagStatsRepository;
import com.ott.batch.repository.WatchHistoryRepository;
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

        // DateTimeFormatter formatter 줄 삭제됨
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

        // 완주율 계산
        BigDecimal completionRate = BigDecimal.ZERO;
        if (!watchHistories.isEmpty()) {
            completionRate = BigDecimal.valueOf(completedCount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(watchHistories.size()), 2, RoundingMode.HALF_UP);
        }

        // 시간대별 집계
        Map<String, Integer> timeSlotCounts = new HashMap<>();
        timeSlotCounts.put("DAWN", 0);
        timeSlotCounts.put("MORNING", 0);
        timeSlotCounts.put("AFTERNOON", 0);
        timeSlotCounts.put("EVENING", 0);
        timeSlotCounts.put("NIGHT", 0);

        for (WatchHistory wh : watchHistories) {
            int hour = wh.getCreatedAt().getHour();
            String timeSlot = getTimeSlot(hour);
            timeSlotCounts.put(timeSlot, timeSlotCounts.get(timeSlot) + 1);
        }

        // 주 시청 시간대
        String peakTimeSlot = timeSlotCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("MORNING");

        // 최장 시청 세션
        int longestSessionSeconds = watchHistories.stream()
                .mapToInt(WatchHistory::getLastPosition)
                .max()
                .orElse(0);

        // 가장 많이 본 태그
        String mostWatchedTagName = tagStatsList.stream()
                .max(Comparator.comparing(TagStats::getTotalViewTime))
                .map(ts -> ts.getTag().getTagName())
                .orElse("없음");

        // 다양성 점수 (시청한 태그 수 * 20)
        int diversityScore = (int) tagStatsList.stream()
                .map(ts -> ts.getTag().getId())
                .distinct()
                .count() * 20;

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
                .dawnCount(timeSlotCounts.get("DAWN"))
                .morningCount(timeSlotCounts.get("MORNING"))
                .afternoonCount(timeSlotCounts.get("AFTERNOON"))
                .eveningCount(timeSlotCounts.get("EVENING"))
                .nightCount(timeSlotCounts.get("NIGHT"))
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