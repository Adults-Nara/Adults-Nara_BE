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
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;
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
 * N+1 문제 해결: 청크 단위로 데이터 일괄 조회 → 집계 → Batch Upsert
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyReportWriter implements ItemWriter<Long> {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final JdbcTemplate jdbcTemplate;
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

        // 4. Batch Upsert
        batchUpsertReports(reports);

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
     * Batch Upsert (TagStatWriter와 동일한 방식)
     */
    private void batchUpsertReports(List<MonthlyReportDto> reports) {
        if (reports.isEmpty()) {
            return;
        }

        String sql = """
            INSERT INTO monthly_watch_report (
                monthly_watch_report_id, user_id, report_year_month,
                total_watch_seconds, total_watch_count, completed_count, completion_rate,
                dawn_count, morning_count, afternoon_count, evening_count, night_count,
                peak_time_slot, longest_session_seconds, most_watched_tag_name, diversity_score,
                created_at, updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
            ON CONFLICT (user_id, report_year_month) DO UPDATE SET
                total_watch_seconds = EXCLUDED.total_watch_seconds,
                total_watch_count = EXCLUDED.total_watch_count,
                completed_count = EXCLUDED.completed_count,
                completion_rate = EXCLUDED.completion_rate,
                dawn_count = EXCLUDED.dawn_count,
                morning_count = EXCLUDED.morning_count,
                afternoon_count = EXCLUDED.afternoon_count,
                evening_count = EXCLUDED.evening_count,
                night_count = EXCLUDED.night_count,
                peak_time_slot = EXCLUDED.peak_time_slot,
                longest_session_seconds = EXCLUDED.longest_session_seconds,
                most_watched_tag_name = EXCLUDED.most_watched_tag_name,
                diversity_score = EXCLUDED.diversity_score,
                updated_at = NOW()
        """;

        jdbcTemplate.batchUpdate(
                sql,
                reports,
                reports.size(),
                (ps, dto) -> {
                    ps.setLong(1, dto.getId());
                    ps.setLong(2, dto.getUserId());
                    ps.setString(3, dto.getReportYearMonth());
                    ps.setLong(4, dto.getTotalWatchSeconds());
                    ps.setInt(5, dto.getTotalWatchCount());
                    ps.setInt(6, dto.getCompletedCount());
                    ps.setBigDecimal(7, dto.getCompletionRate());
                    ps.setInt(8, dto.getDawnCount());
                    ps.setInt(9, dto.getMorningCount());
                    ps.setInt(10, dto.getAfternoonCount());
                    ps.setInt(11, dto.getEveningCount());
                    ps.setInt(12, dto.getNightCount());
                    ps.setString(13, dto.getPeakTimeSlot());
                    ps.setInt(14, dto.getLongestSessionSeconds());
                    ps.setString(15, dto.getMostWatchedTagName());
                    ps.setInt(16, dto.getDiversityScore());
                }
        );

        log.debug("[MonthlyReportWriter] {}건 batch upsert 완료", reports.size());
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