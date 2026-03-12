package com.ott.batch.monthly.step2;

import com.ott.batch.monthly.dto.MonthlyReportDto;
import com.ott.batch.repository.TagStatsRepository;
import com.ott.batch.repository.WatchHistoryRepository;
import com.ott.common.persistence.entity.MonthlyWatchReport;
import com.ott.common.persistence.entity.Tag;
import com.ott.common.persistence.entity.TagStats;
import com.ott.common.persistence.entity.WatchHistory;
import com.ott.common.util.IdGenerator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Step 2: 사용자별 월간 리포트 Writer
 *
 * N+1 문제 해결: 청크 단위로 데이터 일괄 조회 → 집계 → Upsert
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyReportWriter implements ItemWriter<Long> {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @PersistenceContext
    private EntityManager entityManager;

    private final TagStatsRepository tagStatsRepository;
    private final WatchHistoryRepository watchHistoryRepository;

    private LocalDate startDate;
    private LocalDate endDate;
    private OffsetDateTime rangeFrom;
    private OffsetDateTime rangeTo;
    private String yearMonth;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        this.yearMonth = stepExecution.getJobParameters().getString("yearMonth");
        YearMonth ym = YearMonth.parse(yearMonth);

        this.startDate = ym.atDay(1);
        this.endDate = ym.atEndOfMonth();
        this.rangeFrom = OffsetDateTime.parse(stepExecution.getJobParameters().getString("rangeFrom"));
        this.rangeTo = OffsetDateTime.parse(stepExecution.getJobParameters().getString("rangeTo"));

        log.debug("[MonthlyReportWriter] 초기화: yearMonth={}, range=[{}, {})",
                yearMonth, rangeFrom, rangeTo);
    }

    @Override
    public void write(Chunk<? extends Long> chunk) {
        if (chunk.getItems().isEmpty()) {
            return;
        }

        List<Long> userIds = new ArrayList<>(chunk.getItems());

        // 1. 청크의 모든 userId에 대한 TagStats 일괄 조회 (N+1 방지)
        Map<Long, List<TagStats>> tagStatsMap = tagStatsRepository
                .findByUserIdInAndStatsDateBetween(userIds, startDate, endDate)
                .stream()
                .collect(Collectors.groupingBy(ts -> ts.getUser().getId()));

        // 2. 청크의 모든 userId에 대한 WatchHistory 일괄 조회 (N+1 방지)
        Map<Long, List<WatchHistory>> watchHistoryMap = watchHistoryRepository
                .findByUserIdInAndCreatedAtBetween(userIds, rangeFrom, rangeTo)
                .stream()
                .collect(Collectors.groupingBy(wh -> wh.getUser().getId()));

        // 3. 각 userId별로 집계
        List<MonthlyReportDto> reports = new ArrayList<>();
        for (Long userId : userIds) {
            List<TagStats> tagStats = tagStatsMap.getOrDefault(userId, Collections.emptyList());
            List<WatchHistory> watchHistories = watchHistoryMap.getOrDefault(userId, Collections.emptyList());

            if (tagStats.isEmpty() || watchHistories.isEmpty()) {
                log.warn("[MonthlyReportWriter] userId={}의 데이터 없음 (tagStats={}, watchHistory={})",
                        userId, tagStats.size(), watchHistories.size());
                continue;
            }

            MonthlyReportDto report = aggregate(userId, tagStats, watchHistories);
            reports.add(report);
        }

        // 4. Upsert
        upsertReports(reports);

        log.debug("[MonthlyReportWriter] {}개 사용자 처리 완료 ({}개 리포트 생성)",
                userIds.size(), reports.size());
    }

    /**
     * 단일 사용자의 월간 통계 집계
     */
    private MonthlyReportDto aggregate(Long userId, List<TagStats> tagStats, List<WatchHistory> watchHistories) {
        // 전체 시청 시간
        long totalWatchSeconds = watchHistories.stream()
                .mapToLong(WatchHistory::getLastPosition)
                .sum();

        // 완주 수
        int completedCount = (int) watchHistories.stream()
                .filter(WatchHistory::isCompleted)
                .count();

        // 완주율
        BigDecimal completionRate = BigDecimal.valueOf(completedCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(watchHistories.size()), 2, RoundingMode.HALF_UP);

        // 시간대별 집계 (KST 기준)
        Map<String, Long> timeSlotCounts = watchHistories.stream()
                .collect(Collectors.groupingBy(
                        wh -> getTimeSlot(wh.getCreatedAt().atZoneSameInstant(KST).getHour()),
                        Collectors.counting()
                ));

        // 주 시청 시간대
        String peakTimeSlot = timeSlotCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("NONE");

        // 최장 시청 세션
        int longestSessionSeconds = watchHistories.stream()
                .mapToInt(WatchHistory::getLastPosition)
                .max()
                .orElse(0);

        // 가장 많이 본 태그 (월 전체 합산)
        String mostWatchedTagName = tagStats.stream()
                .collect(Collectors.groupingBy(
                        TagStats::getTag,
                        Collectors.summingLong(TagStats::getTotalViewTime)
                ))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> entry.getKey().getTagName())
                .orElse("없음");

        // 다양성 점수 (고유 부모 태그 수 * 20, 최대 100점)
        int diversityScore = (int) tagStats.stream()
                .map(ts -> ts.getTag().getParent())
                .filter(Objects::nonNull)
                .map(Tag::getId)
                .distinct()
                .count() * 20;
        diversityScore = Math.min(100, diversityScore);

        log.debug("[MonthlyReportWriter] userId={}, watchCount={}, completed={}, diversity={}",
                userId, watchHistories.size(), completedCount, diversityScore);

        return MonthlyReportDto.builder()
                .id(IdGenerator.generate())
                .userId(userId)
                .reportYearMonth(yearMonth)
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

    /**
     * 리포트 Upsert (일괄 처리)
     */
    private void upsertReports(List<MonthlyReportDto> reports) {
        if (reports.isEmpty()) {
            return;
        }

        // 기존 리포트 일괄 조회
        List<Long> userIds = reports.stream()
                .map(MonthlyReportDto::getUserId)
                .toList();

        Map<Long, MonthlyWatchReport> existingMap = entityManager
                .createQuery("SELECT m FROM MonthlyWatchReport m WHERE m.userId IN :userIds AND m.reportYearMonth = :yearMonth", MonthlyWatchReport.class)
                .setParameter("userIds", userIds)
                .setParameter("yearMonth", yearMonth)
                .getResultList().stream()
                .collect(Collectors.toMap(MonthlyWatchReport::getUserId, m -> m));

        // Upsert
        for (MonthlyReportDto dto : reports) {
            MonthlyWatchReport existing = existingMap.get(dto.getUserId());
            MonthlyWatchReport entity = dto.toEntity();

            if (existing != null) {
                existing.update(entity);
            } else {
                entityManager.persist(entity);
            }
        }
    }

    /**
     * 시간대 분류
     */
    private String getTimeSlot(int hour) {
        if (hour >= 0 && hour < 6) return "DAWN";
        if (hour >= 6 && hour < 12) return "MORNING";
        if (hour >= 12 && hour < 18) return "AFTERNOON";
        if (hour >= 18 && hour < 22) return "EVENING";
        return "NIGHT";
    }
}