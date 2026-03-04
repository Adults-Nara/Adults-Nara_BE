package com.ott.batch.monthly.step2;

import com.ott.batch.monthly.dto.MonthlyReportDto;
import com.ott.batch.monthly.dto.UserWatchDetailRaw;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Step2 Processor: userId별로 UserWatchDetailRaw를 모아 MonthlyReportDto로 집계.
 *
 * 개선 사항:
 * 1. 시청 기록 단위를 명확히 분리 (lastPosition 기준으로 중복 제거)
 * 2. 태그별 시청 시간은 중복 제거 후 계산
 * 3. 메모리 효율성 개선
 */
@Slf4j
@StepScope
@Component("monthlyReportItemProcessor")
public class MonthlyReportProcessor implements ItemProcessor<UserWatchDetailRaw, MonthlyReportDto> {

    private final String reportYearMonth;

    // ── 상태 버퍼 ──────────────────────────────────────────
    private Long currentUserId = null;
    private final List<UserWatchDetailRaw> buffer = new ArrayList<>();

    public MonthlyReportProcessor(@Value("#{jobParameters['yearMonth']}") String yearMonthStr) {
        this.reportYearMonth = yearMonthStr;
    }

    @Override
    public MonthlyReportDto process(UserWatchDetailRaw raw) {
        if (currentUserId == null) {
            currentUserId = raw.getUserId();
            buffer.add(raw);
            return null;
        }

        if (raw.getUserId().equals(currentUserId)) {
            buffer.add(raw);
            return null;
        }

        // userId가 바뀌었다 → 이전 userId 집계 완료
        MonthlyReportDto result = aggregate(currentUserId, new ArrayList<>(buffer));

        // 새 userId로 전환
        currentUserId = raw.getUserId();
        buffer.clear();
        buffer.add(raw);

        return result;
    }

    public MonthlyReportDto flush() {
        if (currentUserId == null || buffer.isEmpty()) {
            return null;
        }
        MonthlyReportDto result = aggregate(currentUserId, new ArrayList<>(buffer));
        currentUserId = null;
        buffer.clear();
        return result;
    }

    // ── 집계 로직 (개선) ──────────────────────────────────────────

    private MonthlyReportDto aggregate(Long userId, List<UserWatchDetailRaw> rows) {

        // 1. 시청 기록 중복 제거: (lastPosition, completed, watchHour) 조합으로 고유한 시청 단위 식별
        record WatchRecord(int lastPosition, boolean completed, int watchHour) {}

        Map<WatchRecord, UserWatchDetailRaw> uniqueWatchMap = new LinkedHashMap<>();

        for (UserWatchDetailRaw row : rows) {
            WatchRecord key = new WatchRecord(row.getLastPosition(), row.isCompleted(), row.getWatchHour());
            uniqueWatchMap.putIfAbsent(key, row);
        }

        List<UserWatchDetailRaw> uniqueWatches = new ArrayList<>(uniqueWatchMap.values());
        int totalWatchCount = uniqueWatches.size();

        // 2. 기본 지표 계산
        long totalWatchSeconds = uniqueWatches.stream()
                .mapToLong(w -> w.getLastPosition())
                .sum();

        int completedCount = (int) uniqueWatches.stream()
                .filter(UserWatchDetailRaw::isCompleted)
                .count();

        BigDecimal completionRate = totalWatchCount > 0
                ? BigDecimal.valueOf(completedCount * 100.0 / totalWatchCount)
                .setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 3. 시간대 분류
        int dawnCount = 0, morningCount = 0, afternoonCount = 0, eveningCount = 0, nightCount = 0;

        for (UserWatchDetailRaw watch : uniqueWatches) {
            int h = watch.getWatchHour();
            if      (h >= 0  && h <= 5)  dawnCount++;
            else if (h >= 6  && h <= 11) morningCount++;
            else if (h >= 12 && h <= 17) afternoonCount++;
            else if (h >= 18 && h <= 21) eveningCount++;
            else                          nightCount++;
        }

        String peakTimeSlot = determinePeakSlot(dawnCount, morningCount, afternoonCount, eveningCount, nightCount);

        // 4. 최장 시청 시간
        int longestSessionSeconds = uniqueWatches.stream()
                .mapToInt(UserWatchDetailRaw::getLastPosition)
                .max()
                .orElse(0);

        // 5. 최애 태그 (전체 rows에서 태그별 시청 시간 집계)
        Map<String, Integer> tagViewTimeMap = new HashMap<>();
        for (UserWatchDetailRaw row : rows) {
            tagViewTimeMap.merge(row.getTagName(), row.getLastPosition(), Integer::sum);
        }

        String mostWatchedTagName = tagViewTimeMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        // 6. 다양성 점수: 고유 부모태그 수 × 20, 최대 100
        Set<String> uniqueParentTags = new HashSet<>();
        for (UserWatchDetailRaw row : rows) {
            String parentKey = row.getParentTagName() != null
                    ? row.getParentTagName()
                    : row.getTagName();
            uniqueParentTags.add(parentKey);
        }
        int diversityScore = Math.min(100, uniqueParentTags.size() * 20);

        log.debug("[MonthlyReportProcessor] userId={}, watchCount={}, completed={}, diversity={}",
                userId, totalWatchCount, completedCount, diversityScore);

        return MonthlyReportDto.builder()
                .userId(userId)
                .reportYearMonth(reportYearMonth)
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

    private String determinePeakSlot(int dawn, int morning, int afternoon, int evening, int night) {
        Map<String, Integer> slotMap = new LinkedHashMap<>();
        slotMap.put("DAWN", dawn);
        slotMap.put("MORNING", morning);
        slotMap.put("AFTERNOON", afternoon);
        slotMap.put("EVENING", evening);
        slotMap.put("NIGHT", night);

        return slotMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .filter(e -> e.getValue() > 0)
                .map(Map.Entry::getKey)
                .orElse("NONE");
    }
}