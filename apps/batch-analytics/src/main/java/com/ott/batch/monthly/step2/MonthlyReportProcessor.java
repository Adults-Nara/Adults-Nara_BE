package com.ott.batch.monthly.step2;

import com.ott.batch.monthly.dto.MonthlyReportDto;
import com.ott.batch.repository.TagStatsRepository;
import com.ott.batch.repository.WatchHistoryRepository;
import com.ott.common.persistence.entity.TagStats;
import com.ott.common.persistence.entity.WatchHistory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Step 2: 월간 리포트 Processor
 *
 * NOTE: created_at을 시청 시간으로 사용합니다.
 * 더 정확한 통계를 위해서는 WatchHistory에 watched_at 컬럼 추가를 권장합니다.
 */
@Slf4j
@Component
public class MonthlyReportProcessor implements ItemProcessor<Long, MonthlyReportDto>, StepExecutionListener {

    private final TagStatsRepository tagStatsRepository;
    private final WatchHistoryRepository watchHistoryRepository;
    private String yearMonth;
    private LocalDate startDate;
    private LocalDate endDate;
    private OffsetDateTime rangeFrom;
    private OffsetDateTime rangeTo;

    public MonthlyReportProcessor(TagStatsRepository tagStatsRepository,
                                  WatchHistoryRepository watchHistoryRepository) {
        this.tagStatsRepository = tagStatsRepository;
        this.watchHistoryRepository = watchHistoryRepository;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        // Job Parameters에서 yearMonth, rangeFrom, rangeTo 가져오기
        this.yearMonth = stepExecution.getJobParameters().getString("yearMonth");
        String rangeFromStr = stepExecution.getJobParameters().getString("rangeFrom");
        String rangeToStr = stepExecution.getJobParameters().getString("rangeTo");

        this.rangeFrom = OffsetDateTime.parse(rangeFromStr);
        this.rangeTo = OffsetDateTime.parse(rangeToStr);

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

        // 시간대별 집계를 위해 WatchHistory 조회
        List<WatchHistory> watchHistories = watchHistoryRepository.findByUserIdAndCreatedAtBetween(
                userId, rangeFrom, rangeTo
        );

        // 집계 계산
        long totalWatchSeconds = tagStatsList.stream()
                .mapToLong(ts -> ts.getTotalViewTime().longValue())
                .sum();

        int totalWatchCount = tagStatsList.stream()
                .mapToInt(TagStats::getViewCount)
                .sum();

        int completedCount = tagStatsList.stream()
                .mapToInt(TagStats::getCompletedCount)
                .sum();

        BigDecimal completionRate = totalWatchCount > 0
                ? BigDecimal.valueOf(completedCount)
                .divide(BigDecimal.valueOf(totalWatchCount), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 시간대별 집계 (created_at 기반)
        Map<String, Integer> timeSlotCounts = calculateTimeSlotCounts(watchHistories);
        int dawnCount = timeSlotCounts.getOrDefault("DAWN", 0);
        int morningCount = timeSlotCounts.getOrDefault("MORNING", 0);
        int afternoonCount = timeSlotCounts.getOrDefault("AFTERNOON", 0);
        int eveningCount = timeSlotCounts.getOrDefault("EVENING", 0);
        int nightCount = timeSlotCounts.getOrDefault("NIGHT", 0);

        String peakTimeSlot = timeSlotCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("UNKNOWN");

        // 최장 시청 시간 (Integer 오버플로우 방지)
        long maxViewTime = tagStatsList.stream()
                .mapToLong(ts -> ts.getTotalViewTime().longValue())
                .max()
                .orElse(0L);
        int longestSessionSeconds = maxViewTime > Integer.MAX_VALUE
                ? Integer.MAX_VALUE
                : (int) maxViewTime;

        // 최애 태그 (총 시청 시간이 가장 긴 태그)
        String mostWatchedTagName = tagStatsList.stream()
                .max(Comparator.comparing(TagStats::getTotalViewTime))
                .map(ts -> {
                    if (ts.getTag() != null) {
                        return ts.getTag().getTagName();
                    }
                    return "UNKNOWN";
                })
                .orElse("NONE");

        // 다양성 점수 (고유 태그 수 * 20, 최대 100)
        long uniqueTagCount = tagStatsList.stream()
                .map(TagStats::getTag)
                .filter(Objects::nonNull)
                .map(tag -> tag.getId())
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

    /**
     * 시간대별 시청 횟수 계산
     *
     * NOTE: WatchHistory.createdAt을 기준으로 시간대를 분류합니다.
     * 더 정확한 통계를 위해서는 실제 시청 시작 시간(watched_at)이 필요합니다.
     *
     * @param watchHistories 시청 기록 리스트
     * @return 시간대별 시청 횟수 Map
     */
    private Map<String, Integer> calculateTimeSlotCounts(List<WatchHistory> watchHistories) {
        Map<String, Integer> counts = new HashMap<>();
        counts.put("DAWN", 0);
        counts.put("MORNING", 0);
        counts.put("AFTERNOON", 0);
        counts.put("EVENING", 0);
        counts.put("NIGHT", 0);

        for (WatchHistory wh : watchHistories) {
            int hour = wh.getCreatedAt().getHour();
            String timeSlot = getTimeSlot(hour);
            counts.put(timeSlot, counts.get(timeSlot) + 1);
        }

        return counts;
    }

    /**
     * 시간을 기준으로 시간대 분류
     *
     * @param hour 시간 (0-23)
     * @return 시간대 (DAWN/MORNING/AFTERNOON/EVENING/NIGHT)
     */
    private String getTimeSlot(int hour) {
        if (hour >= 0 && hour <= 5) return "DAWN";      // 새벽 (0-5시)
        if (hour >= 6 && hour <= 11) return "MORNING";  // 오전 (6-11시)
        if (hour >= 12 && hour <= 17) return "AFTERNOON"; // 오후 (12-17시)
        if (hour >= 18 && hour <= 21) return "EVENING";  // 저녁 (18-21시)
        return "NIGHT";  // 밤 (22-23시)
    }
}