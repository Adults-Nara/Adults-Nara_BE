package com.ott.core.modules.watch.service;

import com.ott.common.persistence.entity.User;
import com.ott.common.persistence.entity.VideoMetadata;
import com.ott.common.persistence.entity.WatchHistory;
import com.ott.common.persistence.enums.UserRole;
import com.ott.common.util.IdGenerator;
import com.ott.core.modules.user.repository.UserRepository;
import com.ott.core.modules.video.repository.VideoMetadataRepository;
import com.ott.core.modules.video.service.SignedCookieProcessor;
import com.ott.core.modules.watch.dto.response.WatchHistoryItemResponse;
import com.ott.core.modules.watch.dto.response.WatchHistoryPageResponse;
import com.ott.core.modules.watch.repository.WatchHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class GetRecentWatchHistoryTest {

    @MockitoBean private SignedCookieProcessor signedCookieProcessor;
    @MockitoBean private software.amazon.awssdk.services.s3.S3Client s3Client;
    @MockitoBean private software.amazon.awssdk.services.s3.presigner.S3Presigner s3Presigner;

    @Autowired
    private WatchHistoryService watchHistoryService;
    @Autowired private WatchHistoryRepository watchHistoryRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private VideoMetadataRepository videoMetadataRepository;

    private User saveUser(String email, String nickname) {
        return userRepository.save(new User(email, nickname, "pw", UserRole.VIEWER));
    }

    private VideoMetadata saveVideo(Long uploaderId, String title, int duration, boolean deleted) {
        VideoMetadata vm = VideoMetadata.builder()
                .videoId(IdGenerator.generate())
                .userId(uploaderId)
                .title(title)
                .thumbnailUrl("https://thumb.example.com/img.jpg")
                .duration(duration)
                .deleted(deleted)
                .build();
        return videoMetadataRepository.save(vm);
    }

    private void insertHistory(Long userId, Long videoMetadataId, int lastPosition, int duration, OffsetDateTime at) {
        boolean completed = WatchHistory.isVideoCompleted(lastPosition, duration);
        watchHistoryRepository.upsertWatchHistory(IdGenerator.generate(), userId, videoMetadataId, lastPosition, completed, at);
    }

    @Test
    @DisplayName("3개월 이내 시청 이력 정상 조회")
    void 정상조회_응답필드검증() {
        User uploader = saveUser("uploader@test.com", "업로더");
        User viewer = saveUser("viewer@test.com", "시청자");
        VideoMetadata vm = saveVideo(uploader.getId(), "제목", 300, false);

        insertHistory(viewer.getId(), vm.getId(), 150, vm.getDuration(), OffsetDateTime.now(ZoneOffset.UTC).minusDays(10));

        WatchHistoryPageResponse response = watchHistoryService.getRecentWatchHistory(viewer.getId(), 0, 10);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.isHasMore()).isFalse();

        WatchHistoryItemResponse item = response.getItems().getFirst();
        assertThat(item.getVideoId()).isEqualTo(String.valueOf(vm.getVideoId()));
        assertThat(item.getTitle()).isEqualTo("제목");
        assertThat(item.getUploaderName()).isEqualTo("업로더");
        assertThat(item.getWatchProgressPercent()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("3개월 이전 시청 이력 조회")
    void 삼개월_이전데이터조회() {
        User uploader = saveUser("uploader@test.com", "업로더");
        User viewer = saveUser("viewer@test.com", "시청자");
        VideoMetadata vm = saveVideo(uploader.getId(), "제목", 300, false);

        insertHistory(viewer.getId(), vm.getId(), 150, vm.getDuration(), OffsetDateTime.now(ZoneOffset.UTC).minusMonths(4));

        WatchHistoryPageResponse response = watchHistoryService.getRecentWatchHistory(viewer.getId(), 0, 10);

        assertThat(response.getItems()).isEmpty();
        assertThat(response.isHasMore()).isFalse();
    }

    @Test
    @DisplayName("시청 이력이 size를 초과하면 hasMore=true 반환")
    void 다음_페이지_있으면_hasMore_true() {
        User uploader = saveUser("uploader@test.com", "업로더");
        User viewer = saveUser("viewer@test.com", "시청자");
        OffsetDateTime base = OffsetDateTime.now(ZoneOffset.UTC);

        for (int i = 0; i < 11; i++) {
            VideoMetadata vm = saveVideo(uploader.getId(), "영상" + i, 300, false);
            insertHistory(viewer.getId(), vm.getId(), 100, 300, base.minusMinutes(i));
        }

        WatchHistoryPageResponse response = watchHistoryService.getRecentWatchHistory(viewer.getId(), 0, 10);

        assertThat(response.getItems()).hasSize(10);
        assertThat(response.isHasMore()).isTrue();
    }

    @Test
    @DisplayName("마지막 페이지에서는 hasMore=false 반환")
    void 마지막_페이지_hasMore_false() {
        User uploader = saveUser("uploader@test.com", "업로더");
        User viewer = saveUser("viewer@test.com", "시청자");
        OffsetDateTime base = OffsetDateTime.now(ZoneOffset.UTC);

        for (int i = 0; i < 11; i++) {
            VideoMetadata vm = saveVideo(uploader.getId(), "영상" + i, 300, false);
            insertHistory(viewer.getId(), vm.getId(), 100, 300, base.minusMinutes(i));
        }

        // 두 번째 페이지(page=1, size=10) → 나머지 1건
        WatchHistoryPageResponse response = watchHistoryService.getRecentWatchHistory(viewer.getId(), 1, 10);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.isHasMore()).isFalse();
    }

}
