package com.ott.batch.monthly.step2;

import com.ott.batch.monthly.dto.MonthlyReportDto;
import com.ott.batch.monthly.dto.UserWatchDetailRaw;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class MonthlyReportProcessorTest {

    private MonthlyReportProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new MonthlyReportProcessor("2025-02");
    }

    @Test
    @DisplayName("단일 사용자의 시청 기록을 정상적으로 집계한다")
    void aggregateSingleUser() throws Exception {
        // Given: 사용자 1의 시청 기록 3건
        Long userId = 1L;
        List<UserWatchDetailRaw> records = List.of(
                new UserWatchDetailRaw(userId, 1800, true, 10, "드라마", "엔터테인먼트"),
                new UserWatchDetailRaw(userId, 3600, false, 14, "드라마", "엔터테인먼트"),
                new UserWatchDetailRaw(userId, 900, true, 20, "예능", "엔터테인먼트")
        );

        // When: Processor 실행
        MonthlyReportDto result = null;
        for (UserWatchDetailRaw record : records) {
            MonthlyReportDto temp = processor.process(record);
            if (temp != null) result = temp;
        }

        // 마지막 버퍼 flush
        if (result == null) {
            result = processor.flush();
        }

        // Then: 집계 결과 검증
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getTotalWatchCount()).isEqualTo(3);
        assertThat(result.getTotalWatchSeconds()).isEqualTo(6300L);
        assertThat(result.getCompletedCount()).isEqualTo(2);
        assertThat(result.getCompletionRate()).isEqualByComparingTo(BigDecimal.valueOf(66.67));
    }

    @Test
    @DisplayName("동일한 시청 기록(중복)은 한 번만 카운트한다")
    void deduplicateSameWatchRecords() throws Exception {
        // Given: 같은 영상의 태그가 여러 개인 경우
        Long userId = 1L;
        List<UserWatchDetailRaw> records = List.of(
                new UserWatchDetailRaw(userId, 1800, true, 10, "드라마", "엔터테인먼트"),
                new UserWatchDetailRaw(userId, 1800, true, 10, "로맨스", "엔터테인먼트"),
                new UserWatchDetailRaw(userId, 1800, true, 10, "한국드라마", "드라마")
        );

        // When
        MonthlyReportDto result = null;
        for (UserWatchDetailRaw record : records) {
            MonthlyReportDto temp = processor.process(record);
            if (temp != null) result = temp;
        }
        if (result == null) result = processor.flush();

        // Then: 시청 횟수는 1회로 집계
        assertThat(result.getTotalWatchCount()).isEqualTo(1);
        assertThat(result.getTotalWatchSeconds()).isEqualTo(1800L);
        assertThat(result.getCompletedCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("시간대별 시청 횟수를 정확하게 분류한다")
    void classifyWatchTimeSlots() throws Exception {
        // Given: 다양한 시간대의 시청 기록
        Long userId = 1L;
        List<UserWatchDetailRaw> records = List.of(
                new UserWatchDetailRaw(userId, 1800, true, 3,  "드라마", null),  // DAWN
                new UserWatchDetailRaw(userId, 1800, true, 9,  "예능", null),    // MORNING
                new UserWatchDetailRaw(userId, 1800, true, 14, "영화", null),    // AFTERNOON
                new UserWatchDetailRaw(userId, 1800, true, 19, "뉴스", null),    // EVENING
                new UserWatchDetailRaw(userId, 1800, true, 20, "스포츠", null),  // EVENING
                new UserWatchDetailRaw(userId, 1800, true, 22, "다큐", null)     // NIGHT
        );

        // When
        MonthlyReportDto result = null;
        for (UserWatchDetailRaw record : records) {
            MonthlyReportDto temp = processor.process(record);
            if (temp != null) result = temp;
        }
        if (result == null) result = processor.flush();

        // Then
        assertThat(result.getDawnCount()).isEqualTo(1);
        assertThat(result.getMorningCount()).isEqualTo(1);
        assertThat(result.getAfternoonCount()).isEqualTo(1);
        assertThat(result.getEveningCount()).isEqualTo(2);
        assertThat(result.getNightCount()).isEqualTo(1);
        assertThat(result.getPeakTimeSlot()).isEqualTo("EVENING");
    }

    @Test
    @DisplayName("최장 시청 시간을 정확하게 계산한다")
    void calculateLongestSession() throws Exception {
        // Given
        Long userId = 1L;
        List<UserWatchDetailRaw> records = List.of(
                new UserWatchDetailRaw(userId, 1800, true, 10, "드라마", null),
                new UserWatchDetailRaw(userId, 5400, false, 14, "영화", null),
                new UserWatchDetailRaw(userId, 900, true, 20, "예능", null)
        );

        // When
        MonthlyReportDto result = null;
        for (UserWatchDetailRaw record : records) {
            MonthlyReportDto temp = processor.process(record);
            if (temp != null) result = temp;
        }
        if (result == null) result = processor.flush();

        // Then
        assertThat(result.getLongestSessionSeconds()).isEqualTo(5400);
    }

    @Test
    @DisplayName("최애 태그를 시청 시간 기준으로 선정한다")
    void selectMostWatchedTag() throws Exception {
        // Given
        Long userId = 1L;
        List<UserWatchDetailRaw> records = List.of(
                new UserWatchDetailRaw(userId, 1800, true, 10, "드라마", null),
                new UserWatchDetailRaw(userId, 3600, true, 14, "영화", null),
                new UserWatchDetailRaw(userId, 900, true, 20, "예능", null)
        );

        // When
        MonthlyReportDto result = null;
        for (UserWatchDetailRaw record : records) {
            MonthlyReportDto temp = processor.process(record);
            if (temp != null) result = temp;
        }
        if (result == null) result = processor.flush();

        // Then
        assertThat(result.getMostWatchedTagName()).isEqualTo("영화");
    }

    @Test
    @DisplayName("다양성 점수를 고유 부모태그 수 기반으로 계산한다")
    void calculateDiversityScore() throws Exception {
        // Given: 5가지 다른 부모태그
        Long userId = 1L;
        List<UserWatchDetailRaw> records = List.of(
                new UserWatchDetailRaw(userId, 1800, true, 10, "한국드라마", "드라마"),
                new UserWatchDetailRaw(userId, 1800, true, 11, "미국드라마", "드라마"),
                new UserWatchDetailRaw(userId, 1800, true, 12, "액션영화", "영화"),
                new UserWatchDetailRaw(userId, 1800, true, 13, "코미디", "예능"),
                new UserWatchDetailRaw(userId, 1800, true, 14, "뉴스", "시사"),
                new UserWatchDetailRaw(userId, 1800, true, 15, "다큐멘터리", "교양")
        );

        // When
        MonthlyReportDto result = null;
        for (UserWatchDetailRaw record : records) {
            MonthlyReportDto temp = processor.process(record);
            if (temp != null) result = temp;
        }
        if (result == null) result = processor.flush();

        // Then: 5개 부모태그 × 20 = 100점
        assertThat(result.getDiversityScore()).isEqualTo(100);
    }

    @Test
    @DisplayName("시청 기록이 없으면 null을 반환한다")
    void returnNullForNoRecords() throws Exception {
        // Given: 빈 상태

        // When
        MonthlyReportDto result = processor.flush();

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("여러 사용자의 기록을 순차적으로 처리한다")
    void processMultipleUsersSequentially() throws Exception {
        // Given
        List<UserWatchDetailRaw> records = List.of(
                new UserWatchDetailRaw(1L, 1800, true, 10, "드라마", null),
                new UserWatchDetailRaw(1L, 900, true, 14, "예능", null),
                new UserWatchDetailRaw(2L, 3600, true, 20, "영화", null),
                new UserWatchDetailRaw(2L, 1800, false, 22, "뉴스", null)
        );

        // When
        List<MonthlyReportDto> results = new ArrayList<>();
        for (UserWatchDetailRaw record : records) {
            MonthlyReportDto temp = processor.process(record);
            if (temp != null) results.add(temp);
        }
        MonthlyReportDto last = processor.flush();
        if (last != null) results.add(last);

        // Then
        assertThat(results).hasSize(2);

        MonthlyReportDto user1 = results.get(0);
        assertThat(user1.getUserId()).isEqualTo(1L);
        assertThat(user1.getTotalWatchCount()).isEqualTo(2);

        MonthlyReportDto user2 = results.get(1);
        assertThat(user2.getUserId()).isEqualTo(2L);
        assertThat(user2.getTotalWatchCount()).isEqualTo(2);
    }
}