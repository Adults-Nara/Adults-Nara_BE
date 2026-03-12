package com.ott.batch.monthly.step2;

import com.ott.batch.monthly.dto.MonthlyReportDto;
import com.ott.batch.repository.TagStatsRepository;
import com.ott.batch.repository.WatchHistoryRepository;
import com.ott.common.persistence.entity.Tag;
import com.ott.common.persistence.entity.TagStats;
import com.ott.common.persistence.entity.User;
import com.ott.common.persistence.entity.WatchHistory;
import com.ott.common.persistence.entity.VideoMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonthlyReportProcessorTest {

    @Mock
    private TagStatsRepository tagStatsRepository;

    @Mock
    private WatchHistoryRepository watchHistoryRepository;

    private MonthlyReportProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new MonthlyReportProcessor(tagStatsRepository, watchHistoryRepository);

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("yearMonth", "2026-03")
                .addString("rangeFrom", "2026-03-01T00:00:00+09:00")
                .addString("rangeTo", "2026-04-01T00:00:00+09:00")
                .toJobParameters();

        StepExecution stepExecution = mock(StepExecution.class);
        when(stepExecution.getJobParameters()).thenReturn(jobParameters);

        processor.beforeStep(stepExecution);
    }

    @Test
    @DisplayName("단일 사용자의 태그 통계를 정상적으로 집계한다")
    void aggregateSingleUser() throws Exception {
        // Given
        Long userId = 1L;
        User user = createUser(userId);
        Tag tag1 = createTag(10L, "드라마");
        Tag tag2 = createTag(11L, "예능");

        List<TagStats> tagStatsList = Arrays.asList(
                createTagStats(user, tag1, LocalDate.of(2026, 3, 1), 1800L, 1, 1),
                createTagStats(user, tag2, LocalDate.of(2026, 3, 2), 900L, 1, 0)
        );

        when(tagStatsRepository.findByUserIdAndStatsDateBetweenWithTag(
                eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(tagStatsList);

        // WatchHistory Mock - 실제 데이터 반환 (CRITICAL FIX!)
        List<WatchHistory> watchHistories = Arrays.asList(
                createWatchHistory(userId, 1800, true, "2026-03-01T10:00:00+09:00"),
                createWatchHistory(userId, 900, false, "2026-03-02T14:30:00+09:00")
        );
        
        when(watchHistoryRepository.findByUserIdAndCreatedAtBetween(
                eq(userId), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(watchHistories);

        // When
        MonthlyReportDto result = processor.process(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getTotalWatchSeconds()).isEqualTo(2700L);
        assertThat(result.getTotalWatchCount()).isEqualTo(2);
        assertThat(result.getCompletedCount()).isEqualTo(1);
        assertThat(result.getCompletionRate().doubleValue()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("태그 통계가 없으면 null을 반환한다")
    void returnNullForNoTagStats() throws Exception {
        // Given
        Long userId = 1L;
        when(tagStatsRepository.findByUserIdAndStatsDateBetweenWithTag(
                eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        // When
        MonthlyReportDto result = processor.process(userId);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("시청 기록이 없으면 null을 반환한다")
    void returnNullForNoWatchHistory() throws Exception {
        // Given
        Long userId = 1L;
        User user = createUser(userId);
        Tag tag1 = createTag(10L, "드라마");

        List<TagStats> tagStatsList = Arrays.asList(
                createTagStats(user, tag1, LocalDate.of(2026, 3, 1), 1800L, 1, 1)
        );

        when(tagStatsRepository.findByUserIdAndStatsDateBetweenWithTag(
                eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(tagStatsList);

        when(watchHistoryRepository.findByUserIdAndCreatedAtBetween(
                eq(userId), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(Collections.emptyList());

        // When
        MonthlyReportDto result = processor.process(userId);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("최애 태그를 시청 시간 기준으로 선정한다")
    void selectMostWatchedTag() throws Exception {
        // Given
        Long userId = 1L;
        User user = createUser(userId);
        Tag tag1 = createTag(10L, "드라마");
        Tag tag2 = createTag(11L, "영화");
        Tag tag3 = createTag(12L, "예능");

        List<TagStats> tagStatsList = Arrays.asList(
                createTagStats(user, tag1, LocalDate.of(2026, 3, 1), 1800L, 1, 1),
                createTagStats(user, tag2, LocalDate.of(2026, 3, 2), 3600L, 1, 1),  // 최대
                createTagStats(user, tag3, LocalDate.of(2026, 3, 3), 900L, 1, 0)
        );

        when(tagStatsRepository.findByUserIdAndStatsDateBetweenWithTag(
                eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(tagStatsList);

        List<WatchHistory> watchHistories = Arrays.asList(
                createWatchHistory(userId, 1800, true, "2026-03-01T10:00:00+09:00"),
                createWatchHistory(userId, 3600, true, "2026-03-02T10:00:00+09:00"),
                createWatchHistory(userId, 900, false, "2026-03-03T10:00:00+09:00")
        );
        
        when(watchHistoryRepository.findByUserIdAndCreatedAtBetween(
                eq(userId), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(watchHistories);

        // When
        MonthlyReportDto result = processor.process(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMostWatchedTagName()).isEqualTo("영화");
    }

    @Test
    @DisplayName("최장 시청 시간을 정확하게 계산한다")
    void calculateLongestSession() throws Exception {
        // Given
        Long userId = 1L;
        User user = createUser(userId);
        Tag tag1 = createTag(10L, "드라마");

        List<TagStats> tagStatsList = Arrays.asList(
                createTagStats(user, tag1, LocalDate.of(2026, 3, 1), 7200L, 2, 1)
        );

        when(tagStatsRepository.findByUserIdAndStatsDateBetweenWithTag(
                eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(tagStatsList);

        List<WatchHistory> watchHistories = Arrays.asList(
                createWatchHistory(userId, 1800, true, "2026-03-01T10:00:00+09:00"),
                createWatchHistory(userId, 5400, false, "2026-03-01T14:00:00+09:00")  // 최대
        );
        
        when(watchHistoryRepository.findByUserIdAndCreatedAtBetween(
                eq(userId), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(watchHistories);

        // When
        MonthlyReportDto result = processor.process(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getLongestSessionSeconds()).isEqualTo(5400);
    }

    @Test
    @DisplayName("완주율을 정확하게 계산한다")
    void calculateCompletionRate() throws Exception {
        // Given: 5번 시청, 3번 완주 = 60%
        Long userId = 1L;
        User user = createUser(userId);
        Tag tag = createTag(10L, "드라마");

        List<TagStats> tagStatsList = Arrays.asList(
                createTagStats(user, tag, LocalDate.of(2026, 3, 1), 9000L, 5, 3)
        );

        when(tagStatsRepository.findByUserIdAndStatsDateBetweenWithTag(
                eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(tagStatsList);

        List<WatchHistory> watchHistories = Arrays.asList(
                createWatchHistory(userId, 1800, true, "2026-03-01T10:00:00+09:00"),
                createWatchHistory(userId, 1800, true, "2026-03-01T11:00:00+09:00"),
                createWatchHistory(userId, 1800, true, "2026-03-01T12:00:00+09:00"),
                createWatchHistory(userId, 1800, false, "2026-03-01T13:00:00+09:00"),
                createWatchHistory(userId, 1800, false, "2026-03-01T14:00:00+09:00")
        );
        
        when(watchHistoryRepository.findByUserIdAndCreatedAtBetween(
                eq(userId), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(watchHistories);

        // When
        MonthlyReportDto result = processor.process(userId);

        // Then: 3/5 * 100 = 60.0
        assertThat(result).isNotNull();
        assertThat(result.getCompletionRate().doubleValue()).isEqualTo(60.0);
        assertThat(result.getCompletedCount()).isEqualTo(3);
        assertThat(result.getTotalWatchCount()).isEqualTo(5);
    }

    // === Helper Methods ===

    private User createUser(Long userId) {
        User user = new User("test@test.com", "테스터", "kakao", "123");
        try {
            var idField = user.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, userId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return user;
    }

    private Tag createTag(Long tagId, String tagName) {
        Tag tag = new Tag(tagName);
        try {
            var idField = tag.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(tag, tagId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return tag;
    }

    private TagStats createTagStats(User user, Tag tag, LocalDate statsDate,
                                    Long totalViewTime, Integer viewCount, Integer completedCount) {
        TagStats tagStats = new TagStats(tag, user, statsDate);
        try {
            var totalViewTimeField = tagStats.getClass().getDeclaredField("totalViewTime");
            totalViewTimeField.setAccessible(true);
            totalViewTimeField.set(tagStats, totalViewTime);

            var viewCountField = tagStats.getClass().getDeclaredField("viewCount");
            viewCountField.setAccessible(true);
            viewCountField.set(tagStats, viewCount);

            var completedCountField = tagStats.getClass().getDeclaredField("completedCount");
            completedCountField.setAccessible(true);
            completedCountField.set(tagStats, completedCount);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return tagStats;
    }

    private WatchHistory createWatchHistory(Long userId, int lastPosition, boolean completed, String createdAt) {
        try {
            VideoMetadata video = VideoMetadata.builder()
                    .title("테스트 비디오")
                    .duration(3600)
                    .build();
            
            var videoIdField = video.getClass().getDeclaredField("id");
            videoIdField.setAccessible(true);
            videoIdField.set(video, 100L);

            User user = new User("test@test.com", "테스터", "kakao", "123");
                    var userIdField = user.getClass().getDeclaredField("id");
                    userIdField.setAccessible(true);
                    userIdField.set(user, userId);
                    
                    WatchHistory wh = new WatchHistory(user, video, lastPosition);
            
            // completed 필드 설정
            var completedField = wh.getClass().getDeclaredField("completed");
            completedField.setAccessible(true);
            completedField.set(wh, completed);
            
            var createdAtField = wh.getClass().getSuperclass().getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(wh, OffsetDateTime.parse(createdAt));
            
            return wh;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
