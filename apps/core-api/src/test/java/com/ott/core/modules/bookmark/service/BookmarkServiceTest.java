package com.ott.core.modules.bookmark.service;

import com.ott.common.persistence.entity.Bookmark;
import com.ott.common.persistence.entity.User;
import com.ott.common.persistence.entity.VideoMetadata;
import com.ott.common.persistence.entity.WatchHistory;
import com.ott.common.persistence.enums.UserRole;
import com.ott.common.persistence.enums.VideoType;
import com.ott.common.util.IdGenerator;
import com.ott.core.modules.bookmark.dto.BookmarkPageResponse;
import com.ott.core.modules.bookmark.dto.BookmarkSummaryResponse;
import com.ott.core.modules.bookmark.repository.BookmarkRepository;
import com.ott.core.modules.user.repository.UserRepository;
import com.ott.core.modules.video.repository.VideoMetadataRepository;
import com.ott.core.modules.video.service.SignedCookieProcessor;
import com.ott.core.modules.watch.repository.WatchHistoryRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class BookmarkServiceTest {

    @MockitoBean private SignedCookieProcessor signedCookieProcessor;
    @MockitoBean private software.amazon.awssdk.services.s3.S3Client s3Client;
    @MockitoBean private software.amazon.awssdk.services.s3.presigner.S3Presigner s3Presigner;

    @Autowired private EntityManager em;
    @Autowired private BookmarkService bookmarkService;
    @Autowired private UserRepository userRepository;
    @Autowired private VideoMetadataRepository videoMetadataRepository;
    @Autowired private BookmarkRepository bookmarkRepository;
    @Autowired private WatchHistoryRepository watchHistoryRepository;

    private User saveUser(String email, String nickname) {
        return userRepository.save(new User(email, nickname, "pw", UserRole.VIEWER));
    }

    private VideoMetadata saveVideo(Long uploaderId, VideoType videoType, String thumbnailUrl) {
        return videoMetadataRepository.save(VideoMetadata.builder()
                .videoId(IdGenerator.generate())
                .userId(uploaderId)
                .title("테스트 영상")
                .thumbnailUrl(thumbnailUrl)
                .viewCount(100)
                .duration(300)
                .videoType(videoType)
                .deleted(false)
                .build());
    }

    private void saveBookmark(User user, VideoMetadata vm) {
        bookmarkRepository.save(new Bookmark(user, vm));
    }

    @Test
    @DisplayName("숏폼과 롱폼 재생목록을 각각 반환")
    void 숏폼_롱폼_각각반환() {
        User user = saveUser("user@test.com", "유저");
        saveBookmark(user, saveVideo(user.getId(), VideoType.SHORT, "https://thumb.com/short.jpg"));
        saveBookmark(user, saveVideo(user.getId(), VideoType.LONG, "https://thumb.com/long.jpg"));

        em.flush();
        em.clear();

        BookmarkSummaryResponse result = bookmarkService.getBookmarkSummary(user.getId());

        assertThat(result.getShortForm().getTotalCount()).isEqualTo(1);
        assertThat(result.getLongForm().getTotalCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("썸네일은 최대 4개만 반환")
    void 썸네일_최대4개_반환() {
        User user = saveUser("user@test.com", "유저");
        for (int i = 1; i <= 10; i++) {
            saveBookmark(user, saveVideo(user.getId(), VideoType.SHORT, "https://thumb.com/" + i + ".jpg"));
        }

        em.flush();
        em.clear();

        BookmarkSummaryResponse result = bookmarkService.getBookmarkSummary(user.getId());

        assertThat(result.getShortForm().getThumbnails()).hasSize(4);
        assertThat(result.getShortForm().getTotalCount()).isEqualTo(10);
    }

    @Test
    @DisplayName("찜이 없으면 빈 목록과 개수 0을 반환")
    void 찜없음_빈목록_0개반환() {
        User user = saveUser("user@test.com", "유저");

        em.flush();
        em.clear();

        BookmarkSummaryResponse result = bookmarkService.getBookmarkSummary(user.getId());

        assertThat(result.getShortForm().getThumbnails()).isEmpty();
        assertThat(result.getShortForm().getTotalCount()).isZero();
        assertThat(result.getLongForm().getThumbnails()).isEmpty();
        assertThat(result.getLongForm().getTotalCount()).isZero();
    }

    @Test
    @DisplayName("삭제된 영상은 조회에서 제외")
    void 삭제영상_조회제외() {
        User user = saveUser("user@test.com", "유저");
        saveBookmark(user, saveVideo(user.getId(), VideoType.SHORT, "https://thumb.com/active.jpg"));
        VideoMetadata deletedVm = videoMetadataRepository.save(VideoMetadata.builder()
                .videoId(IdGenerator.generate())
                .userId(user.getId())
                .title("삭제된 영상")
                .thumbnailUrl("https://thumb.com/deleted.jpg")
                .videoType(VideoType.SHORT)
                .deleted(true)
                .build());
        saveBookmark(user, deletedVm);

        em.flush();
        em.clear();

        BookmarkSummaryResponse result = bookmarkService.getBookmarkSummary(user.getId());

        assertThat(result.getShortForm().getTotalCount()).isEqualTo(1);
        assertThat(result.getShortForm().getThumbnails()).containsExactly("https://thumb.com/active.jpg");
    }

    @Test
    @DisplayName("다음 페이지가 있으면 hasMore이 true")
    void hasMore_true() {
        User user = saveUser("user@test.com", "유저");
        for (int i = 1; i <= 11; i++) {
            saveBookmark(user, saveVideo(user.getId(), VideoType.SHORT, "https://thumb.com/" + i + ".jpg"));
        }

        em.flush();
        em.clear();

        BookmarkPageResponse result = bookmarkService.getBookmarkList(user.getId(), VideoType.SHORT, 0, 10);

        assertThat(result.isHasMore()).isTrue();
        assertThat(result.getItems()).hasSize(10);
    }

    @Test
    @DisplayName("마지막 페이지이면 hasMore이 false")
    void hasMore_false() {
        User user = saveUser("user@test.com", "유저");
        for (int i = 1; i <= 3; i++) {
            saveBookmark(user, saveVideo(user.getId(), VideoType.SHORT, "https://thumb.com/" + i + ".jpg"));
        }

        em.flush();
        em.clear();

        BookmarkPageResponse result = bookmarkService.getBookmarkList(user.getId(), VideoType.SHORT, 0, 10);

        assertThat(result.isHasMore()).isFalse();
        assertThat(result.getItems()).hasSize(3);
    }

    @Test
    @DisplayName("시청 이력이 있으면 진행률 계산")
    void 시청진행률_계산() {
        User user = saveUser("user@test.com", "유저");
        VideoMetadata vm = saveVideo(user.getId(), VideoType.SHORT, "https://thumb.com/1.jpg"); // duration = 300
        saveBookmark(user, vm);
        watchHistoryRepository.save(new WatchHistory(user, vm, 150)); // 150 / 300 = 50%

        em.flush();
        em.clear();

        BookmarkPageResponse result = bookmarkService.getBookmarkList(user.getId(), VideoType.SHORT, 0, 10);

        assertThat(result.getItems().get(0).getWatchProgressPercent()).isEqualTo(50.0);
    }
}