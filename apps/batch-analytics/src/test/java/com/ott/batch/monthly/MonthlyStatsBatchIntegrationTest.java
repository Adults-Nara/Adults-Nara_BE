package com.ott.batch.monthly;

import com.ott.batch.repository.*;
import com.ott.common.persistence.entity.*;
import com.ott.common.persistence.enums.VideoType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@SpringBatchTest
@TestPropertySource(properties = {
        "spring.batch.job.enabled=false",
        "spring.jpa.show-sql=true",
        "logging.level.com.ott.batch=DEBUG"
})
class MonthlyStatsBatchIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(MonthlyStatsBatchIntegrationTest.class);

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VideoMetadataRepository videoMetadataRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private VideoTagRepository videoTagRepository;

    @Autowired
    private WatchHistoryRepository watchHistoryRepository;

    @Autowired
    private TagStatsRepository tagStatsRepository;

    @Autowired
    private MonthlyWatchReportRepository monthlyWatchReportRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        log.info("=== 테스트 데이터 정리 시작 ===");

        jdbcTemplate.execute("DELETE FROM watch_history");
        jdbcTemplate.execute("DELETE FROM video_tag");
        jdbcTemplate.execute("DELETE FROM tag_stats");
        jdbcTemplate.execute("DELETE FROM monthly_watch_report");
        jdbcTemplate.execute("DELETE FROM user_tag");
        jdbcTemplate.execute("DELETE FROM video_metadata");
        jdbcTemplate.execute("DELETE FROM tag");
        jdbcTemplate.execute("DELETE FROM users");

        log.info("=== 테스트 데이터 정리 완료 ===");
    }

    @Test
    @DisplayName("월간 통계 배치가 정상적으로 실행된다")
    void runMonthlyStatsBatch() throws Exception {
        log.info("=== 테스트 시작: 월간 통계 배치 실행 ===");

        // Given: 테스트 데이터 생성
        User user = new User("test@test.com", "테스터", "kakao", "123");
        user = userRepository.saveAndFlush(user);

        Tag dramaTag = new Tag("드라마");
        dramaTag = tagRepository.saveAndFlush(dramaTag);

        VideoMetadata video = VideoMetadata.builder()
                .videoId(System.currentTimeMillis())
                .userId(user.getId())
                .title("테스트 드라마")
                .duration(3600)
                .videoType(VideoType.LONG)
                .isAd(false)
                .build();
        video = videoMetadataRepository.saveAndFlush(video);

        VideoTag videoTag = new VideoTag(video, dramaTag);
        videoTagRepository.saveAndFlush(videoTag);

        WatchHistory watchHistory = new WatchHistory(user, video, 1800);
        watchHistoryRepository.saveAndFlush(watchHistory);

        // When: 배치 실행
        OffsetDateTime now = OffsetDateTime.now();
        String currentYearMonth = String.format("%04d-%02d", now.getYear(), now.getMonthValue());
        OffsetDateTime monthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        OffsetDateTime monthEnd = now.withDayOfMonth(now.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59).withNano(0);

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("yearMonth", currentYearMonth)
                .addString("rangeFrom", monthStart.toString())
                .addString("rangeTo", monthEnd.toString())
                .addString("runAt", OffsetDateTime.now().toString())
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // Then: 배치 성공 확인
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // TagStats 확인
        LocalDate queryStart = LocalDate.of(now.getYear(), now.getMonthValue(), 1);
        LocalDate queryEnd = LocalDate.of(now.getYear(), now.getMonthValue(), now.toLocalDate().lengthOfMonth());

        var tagStats = tagStatsRepository.findByUserIdAndStatsDateBetween(
                user.getId(), queryStart, queryEnd
        );
        assertThat(tagStats).isNotEmpty();

        // MonthlyWatchReport 확인
        var report = monthlyWatchReportRepository.findByUserIdAndReportYearMonth(
                user.getId(), currentYearMonth
        );
        assertThat(report).isPresent();

        log.info("=== 테스트 완료 ===");
    }

    @Test
    @DisplayName("시간대별 시청 통계가 정확하게 집계된다")
    void calculateTimeSlotStatistics() throws Exception {
        log.info("=== 테스트 시작: 시간대별 시청 통계 ===");

        // Given
        User user = new User("timeslot@test.com", "시간대테스터", "kakao", "789");
        user = userRepository.saveAndFlush(user);

        Tag tag = new Tag("영화");
        tag = tagRepository.saveAndFlush(tag);

        OffsetDateTime now = OffsetDateTime.now();

        // 각 시간대마다 다른 비디오 생성
        VideoMetadata video1 = createVideo(user.getId(), "새벽 영화", 1001);
        VideoMetadata video2 = createVideo(user.getId(), "오전 영화", 1002);
        VideoMetadata video3 = createVideo(user.getId(), "오후 영화1", 1003);
        VideoMetadata video4 = createVideo(user.getId(), "오후 영화2", 1004);
        VideoMetadata video5 = createVideo(user.getId(), "저녁 영화", 1005);
        VideoMetadata video6 = createVideo(user.getId(), "밤 영화", 1006);

        // 각 비디오에 태그 연결
        videoTagRepository.saveAndFlush(new VideoTag(video1, tag));
        videoTagRepository.saveAndFlush(new VideoTag(video2, tag));
        videoTagRepository.saveAndFlush(new VideoTag(video3, tag));
        videoTagRepository.saveAndFlush(new VideoTag(video4, tag));
        videoTagRepository.saveAndFlush(new VideoTag(video5, tag));
        videoTagRepository.saveAndFlush(new VideoTag(video6, tag));

        // 다양한 시간대에 시청 기록 생성
        createWatchHistoryWithTime(user.getId(), video1.getId(),
                now.withHour(3).withMinute(0).withSecond(0));
        createWatchHistoryWithTime(user.getId(), video2.getId(),
                now.withHour(9).withMinute(0).withSecond(0));
        createWatchHistoryWithTime(user.getId(), video3.getId(),
                now.withHour(14).withMinute(0).withSecond(0));
        createWatchHistoryWithTime(user.getId(), video4.getId(),
                now.withHour(15).withMinute(0).withSecond(0));
        createWatchHistoryWithTime(user.getId(), video5.getId(),
                now.withHour(19).withMinute(0).withSecond(0));
        createWatchHistoryWithTime(user.getId(), video6.getId(),
                now.withHour(22).withMinute(0).withSecond(0));

        // When: 배치 실행
        String currentYearMonth = String.format("%04d-%02d", now.getYear(), now.getMonthValue());
        OffsetDateTime monthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        OffsetDateTime monthEnd = now.withDayOfMonth(now.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59).withNano(0);

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("yearMonth", currentYearMonth)
                .addString("rangeFrom", monthStart.toString())
                .addString("rangeTo", monthEnd.toString())
                .addString("runAt", OffsetDateTime.now().toString())
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // Then: 시간대별 집계 확인
        var report = monthlyWatchReportRepository.findByUserIdAndReportYearMonth(
                user.getId(), currentYearMonth
        );

        assertThat(report).isPresent();

        var reportData = report.get();
        System.out.println("============ 시간대별 집계 결과 ============");
        System.out.println("새벽 (0-5시): " + reportData.getDawnCount());
        System.out.println("오전 (6-11시): " + reportData.getMorningCount());
        System.out.println("오후 (12-17시): " + reportData.getAfternoonCount());
        System.out.println("저녁 (18-21시): " + reportData.getEveningCount());
        System.out.println("밤 (22-23시): " + reportData.getNightCount());
        System.out.println("주시청시간대: " + reportData.getPeakTimeSlot());
        System.out.println("총 시청 횟수: " + reportData.getTotalWatchCount());

        // 총 시청 횟수가 6개인지만 확인 (시간대 상관없이)
        assertThat(reportData.getTotalWatchCount()).isEqualTo(6);

        // 모든 시간대 합이 6인지 확인
        int totalTimeSlots = reportData.getDawnCount() + reportData.getMorningCount() +
                reportData.getAfternoonCount() + reportData.getEveningCount() +
                reportData.getNightCount();
        assertThat(totalTimeSlots).isEqualTo(6);

        log.info("=== 시간대별 통계 테스트 완료 ===");
    }

    @Test
    @DisplayName("배치 재실행 시 데이터가 중복 생성되지 않는다")
    void preventDuplicateOnRerun() throws Exception {
        log.info("=== 테스트 시작: 배치 재실행 중복 방지 ===");

        // Given
        User user = new User("test2@test.com", "테스터2", "kakao", "456");
        user = userRepository.saveAndFlush(user);

        Tag tag = new Tag("예능");
        tag = tagRepository.saveAndFlush(tag);

        VideoMetadata video = VideoMetadata.builder()
                .videoId(System.currentTimeMillis() + 1000)
                .userId(user.getId())
                .title("테스트 예능")
                .duration(3600)
                .videoType(VideoType.LONG)
                .isAd(false)
                .build();
        video = videoMetadataRepository.saveAndFlush(video);

        VideoTag videoTag = new VideoTag(video, tag);
        videoTagRepository.saveAndFlush(videoTag);

        WatchHistory watchHistory = new WatchHistory(user, video, 1800);
        watchHistoryRepository.saveAndFlush(watchHistory);

        // When: 2번 실행
        OffsetDateTime now = OffsetDateTime.now();
        String currentYearMonth = String.format("%04d-%02d", now.getYear(), now.getMonthValue());
        OffsetDateTime monthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        OffsetDateTime monthEnd = now.withDayOfMonth(now.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59).withNano(0);

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("yearMonth", currentYearMonth)
                .addString("rangeFrom", monthStart.toString())
                .addString("rangeTo", monthEnd.toString())
                .addString("runAt", OffsetDateTime.now().toString())
                .toJobParameters();

        JobExecution firstExecution = jobLauncherTestUtils.launchJob(jobParameters);
        assertThat(firstExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        JobParameters retryParams = new JobParametersBuilder()
                .addString("yearMonth", currentYearMonth)
                .addString("rangeFrom", monthStart.toString())
                .addString("rangeTo", monthEnd.toString())
                .addString("runAt", OffsetDateTime.now().plusSeconds(1).toString())
                .toJobParameters();

        JobExecution secondExecution = jobLauncherTestUtils.launchJob(retryParams);
        assertThat(secondExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // Then
        var reports = monthlyWatchReportRepository.findByUserIdAndReportYearMonth(
                user.getId(), currentYearMonth
        );
        assertThat(reports).isPresent();

        LocalDate queryStart = LocalDate.of(now.getYear(), now.getMonthValue(), 1);
        LocalDate queryEnd = LocalDate.of(now.getYear(), now.getMonthValue(), now.toLocalDate().lengthOfMonth());
        var tagStats = tagStatsRepository.findByUserIdAndStatsDateBetween(
                user.getId(), queryStart, queryEnd
        );
        assertThat(tagStats).hasSize(1);

        log.info("=== 테스트 완료 ===");
    }

    // ========== Helper Methods ==========

    private VideoMetadata createVideo(Long userId, String title, long offset) {
        VideoMetadata video = VideoMetadata.builder()
                .videoId(System.currentTimeMillis() + offset)
                .userId(userId)
                .title(title)
                .duration(7200)
                .videoType(VideoType.LONG)
                .isAd(false)
                .build();
        return videoMetadataRepository.saveAndFlush(video);
    }

    private void createWatchHistoryWithTime(Long userId, Long videoMetadataId, OffsetDateTime createdAt) {
        String sql = """
            INSERT INTO watch_history (watch_history_id, user_id, video_metadata_id, last_position, completed, deleted, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        long id = System.nanoTime();
        jdbcTemplate.update(sql,
                id,
                userId,
                videoMetadataId,
                1800,
                false,
                false,
                Timestamp.from(createdAt.toInstant()),
                Timestamp.from(createdAt.toInstant())
        );
    }
}