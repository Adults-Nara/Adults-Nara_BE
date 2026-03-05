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
import org.springframework.test.context.TestPropertySource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

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
        log.info("User 생성 중...");
        User user = new User("test@test.com", "테스터", "kakao", "123");
        user = userRepository.saveAndFlush(user);
        log.info("User 생성 완료: userId={}", user.getId());

        log.info("Tag 생성 중...");
        Tag dramaTag = new Tag("드라마");
        dramaTag = tagRepository.saveAndFlush(dramaTag);
        log.info("Tag 생성 완료: tagId={}", dramaTag.getId());

        log.info("VideoMetadata 생성 중...");
        VideoMetadata video = VideoMetadata.builder()
                .userId(user.getId())
                .title("테스트 드라마")
                .duration(3600)
                .videoType(VideoType.LONG)  // VOD → LONG
                .isAd(false)
                .build();
        video = videoMetadataRepository.saveAndFlush(video);
        log.info("VideoMetadata 생성 완료: videoMetadataId={}", video.getId());

        log.info("VideoTag 생성 중...");
        VideoTag videoTag = new VideoTag(video, dramaTag);
        videoTagRepository.saveAndFlush(videoTag);
        log.info("VideoTag 생성 완료");

        log.info("WatchHistory 생성 중...");
        WatchHistory watchHistory = new WatchHistory(user, video, 1800);
        watchHistoryRepository.saveAndFlush(watchHistory);
        log.info("WatchHistory 생성 완료");

        // When: 배치 실행 (현재 월 기준)
        log.info("배치 파라미터 생성 중...");
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
        System.out.println("배치 파라미터: yearMonth=" + currentYearMonth +
                ", rangeFrom=" + monthStart + ", rangeTo=" + monthEnd);
        log.info("배치 파라미터: {}", jobParameters);

        log.info("배치 실행 시작...");
        System.out.println("============ 배치 실행 시작 ============");
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
        System.out.println("배치 실행 완료: status=" + jobExecution.getStatus());
        System.out.println("배치 Exit Status: " + jobExecution.getExitStatus());
        log.info("배치 실행 완료: status={}", jobExecution.getStatus());
        log.info("배치 Exit Status: {}", jobExecution.getExitStatus());

        if (jobExecution.getStatus() != BatchStatus.COMPLETED) {
            System.out.println("============ 배치 실행 실패 ============");
            jobExecution.getStepExecutions().forEach(step -> {
                System.out.println("Step 실패: " + step.getStepName() +
                        ", status=" + step.getStatus() +
                        ", exitStatus=" + step.getExitStatus() +
                        ", readCount=" + step.getReadCount() +
                        ", writeCount=" + step.getWriteCount());
                step.getFailureExceptions().forEach(e -> {
                    System.out.println("에러: " + e.getMessage());
                    e.printStackTrace();
                });
                log.error("Step 실패: name={}, status={}, exitStatus={}, readCount={}, writeCount={}, errors={}",
                        step.getStepName(),
                        step.getStatus(),
                        step.getExitStatus(),
                        step.getReadCount(),
                        step.getWriteCount(),
                        step.getFailureExceptions());
            });
        } else {
            System.out.println("============ 배치 실행 성공 ============");
        }

        // Then: 배치 성공 확인
        assertThat(jobExecution.getStatus())
                .withFailMessage("배치 실행 실패: status=%s, exitStatus=%s",
                        jobExecution.getStatus(),
                        jobExecution.getExitStatus())
                .isEqualTo(BatchStatus.COMPLETED);

        // Step 결과 확인
        System.out.println("============ TagStats 조회 ============");
        log.info("TagStats 조회 중...");
        LocalDate queryStart = LocalDate.of(now.getYear(), now.getMonthValue(), 1);
        LocalDate queryEnd = LocalDate.of(now.getYear(), now.getMonthValue(), now.toLocalDate().lengthOfMonth());
        System.out.println("조회 기간: " + queryStart + " ~ " + queryEnd);

        var tagStats = tagStatsRepository.findByUserIdAndStatsDateBetween(
                user.getId(),
                queryStart,
                queryEnd
        );
        System.out.println("TagStats 개수: " + tagStats.size());
        log.info("TagStats 조회 완료: count={}", tagStats.size());
        if (tagStats.isEmpty()) {
            System.out.println("ERROR: TagStats가 비어있습니다!");
            log.error("TagStats가 비어있습니다!");
        } else {
            tagStats.forEach(ts -> {
                System.out.println("TagStats: statsDate=" + ts.getStatsDate() + ", viewCount=" + ts.getViewCount());
                log.info("TagStats: statsDate={}, viewCount={}", ts.getStatsDate(), ts.getViewCount());
            });
        }
        assertThat(tagStats)
                .withFailMessage("TagStats가 생성되지 않았습니다")
                .isNotEmpty();

        System.out.println("============ MonthlyWatchReport 조회 ============");
        log.info("MonthlyWatchReport 조회 중...");
        var report = monthlyWatchReportRepository.findByUserIdAndReportYearMonth(
                user.getId(), currentYearMonth
        );
        System.out.println("MonthlyWatchReport exists: " + report.isPresent());
        log.info("MonthlyWatchReport 조회 완료: exists={}", report.isPresent());
        if (report.isEmpty()) {
            System.out.println("ERROR: MonthlyWatchReport가 생성되지 않았습니다!");
            log.error("MonthlyWatchReport가 생성되지 않았습니다!");
        } else {
            System.out.println("Report: totalWatchCount=" + report.get().getTotalWatchCount() +
                    ", totalWatchSeconds=" + report.get().getTotalWatchSeconds() +
                    ", completedCount=" + report.get().getCompletedCount());
            log.info("Report: totalWatchCount={}, totalWatchSeconds={}, completedCount={}",
                    report.get().getTotalWatchCount(),
                    report.get().getTotalWatchSeconds(),
                    report.get().getCompletedCount());
        }
        assertThat(report)
                .withFailMessage("MonthlyWatchReport가 생성되지 않았습니다")
                .isPresent();

        log.info("=== 테스트 완료 ===");
    }

    @Test
    @DisplayName("배치 재실행 시 데이터가 중복 생성되지 않는다")
    void preventDuplicateOnRerun() throws Exception {
        log.info("=== 테스트 시작: 배치 재실행 중복 방지 ===");

        // Given
        User user = new User("test2@test.com", "테스터2", "kakao", "456");
        user = userRepository.saveAndFlush(user);
        log.info("User 생성: userId={}", user.getId());

        Tag tag = new Tag("예능");
        tag = tagRepository.saveAndFlush(tag);
        log.info("Tag 생성: tagId={}", tag.getId());

        VideoMetadata video = VideoMetadata.builder()
                .userId(user.getId())
                .title("테스트 예능")
                .duration(3600)
                .videoType(VideoType.LONG)  // VOD → LONG
                .isAd(false)
                .build();
        video = videoMetadataRepository.saveAndFlush(video);
        log.info("VideoMetadata 생성: videoMetadataId={}", video.getId());

        VideoTag videoTag = new VideoTag(video, tag);
        videoTagRepository.saveAndFlush(videoTag);
        log.info("VideoTag 생성 완료");

        WatchHistory watchHistory = new WatchHistory(user, video, 1800);
        watchHistoryRepository.saveAndFlush(watchHistory);
        log.info("WatchHistory 생성 완료");

        // 현재 월 기준으로 배치 파라미터 생성
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

        // When: 2번 실행
        log.info("첫 번째 배치 실행 시작...");
        JobExecution firstExecution = jobLauncherTestUtils.launchJob(jobParameters);
        log.info("첫 번째 배치 실행 완료: status={}", firstExecution.getStatus());

        JobParameters retryParams = new JobParametersBuilder()
                .addString("yearMonth", currentYearMonth)
                .addString("rangeFrom", monthStart.toString())
                .addString("rangeTo", monthEnd.toString())
                .addString("runAt", OffsetDateTime.now().plusSeconds(1).toString())
                .toJobParameters();

        log.info("두 번째 배치 실행 시작...");
        JobExecution secondExecution = jobLauncherTestUtils.launchJob(retryParams);
        log.info("두 번째 배치 실행 완료: status={}", secondExecution.getStatus());

        // Then
        assertThat(secondExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        var reports = monthlyWatchReportRepository.findByUserIdAndReportYearMonth(
                user.getId(), currentYearMonth
        );
        assertThat(reports).isPresent();

        LocalDate queryStart = LocalDate.of(now.getYear(), now.getMonthValue(), 1);
        LocalDate queryEnd = LocalDate.of(now.getYear(), now.getMonthValue(), now.toLocalDate().lengthOfMonth());
        var tagStats = tagStatsRepository.findByUserIdAndStatsDateBetween(
                user.getId(),
                queryStart,
                queryEnd
        );
        log.info("TagStats 최종 개수: {}", tagStats.size());
        assertThat(tagStats).hasSize(1);

        log.info("=== 테스트 완료 ===");
    }
}