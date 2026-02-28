package com.ott.core.modules.bookmark.service;

import com.ott.common.error.BusinessException;
import com.ott.common.error.ErrorCode;
import com.ott.common.persistence.entity.Bookmark;
import com.ott.common.persistence.entity.User;
import com.ott.common.persistence.entity.VideoMetadata;
import com.ott.common.persistence.entity.WatchHistory;
import com.ott.common.persistence.enums.VideoType;
import com.ott.core.modules.bookmark.dto.BookmarkListResponse;
import com.ott.core.modules.bookmark.dto.BookmarkPageResponse;
import com.ott.core.modules.bookmark.dto.BookmarkPlaylistResponse;
import com.ott.core.modules.bookmark.dto.BookmarkSummaryResponse;
import com.ott.core.modules.bookmark.repository.BookmarkRepository;
import com.ott.core.modules.user.repository.UserRepository;
import com.ott.core.modules.video.repository.VideoMetadataRepository;
import com.ott.core.modules.watch.repository.WatchHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;
    private final VideoMetadataRepository videoMetadataRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final WatchHistoryRepository watchHistoryRepository;


    // Redis Key 상수
    private static final String KEY_VIDEO_COUNT = "video:count:bookmark"; // Hash 구조
    private static final String KEY_RANKING = "video:ranking"; // ZSet 구조
    private static final String KEY_DIRTY_DATA = "video:dirty:bookmark"; // 변경된 영상 ID 목록 (Set)

    @Transactional
    public void toggleBookmark(Long userId, Long videoId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        VideoMetadata metadata = videoMetadataRepository.findByVideoId(videoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VIDEO_NOT_FOUND));

        Optional<Bookmark> existingBookmark = bookmarkRepository.findByUserAndVideoMetadata(user, metadata);
        try {
            if (existingBookmark.isPresent()) {
                // 이미 찜했으면 -> 취소
                bookmarkRepository.delete(existingBookmark.get());
                bookmarkRepository.flush(); // DB에 쿼리를 즉시 날려 예외가 있는지 먼저 확인
                updateRedis(videoId, -1);   // 예외가 안 터졌을 때만 Redis 연산 실행 (안전 보장)
            } else {
                // 없으면 -> 찜하기
                Bookmark newBookmark = new Bookmark(user, metadata);
                bookmarkRepository.save(newBookmark);
                bookmarkRepository.flush(); // DB 유니크 제약조건 위반 검사
                updateRedis(videoId, 1);    // 정상 처리 시에만 Redis 연산 실행
            }
        } catch (DataIntegrityViolationException e) {

            log.warn("[Bookmark] 동시 요청으로 인한 중복 방어 - userId: {}, videoId: {}", userId, videoId);
            throw new BusinessException(ErrorCode.BOOKMARK_CONFLICT);
        }
    }

    @Transactional(readOnly = true)
    public boolean isBookmarked(Long userId, Long videoId) {
        return bookmarkRepository.existsByUserIdAndVideoMetadata_VideoId(userId, videoId);
    }

    private void updateRedis(Long videoId, int delta) {
        String videoIdStr = String.valueOf(videoId);

        // 화면 표시용 카운트 (Hash) -> 상세 페이지에서 보여줄 숫자
        stringRedisTemplate.opsForHash().increment(KEY_VIDEO_COUNT, videoIdStr, delta);

        // 인기 차트용 점수 (Sorted Set) -> 실시간 랭킹 반영
        stringRedisTemplate.opsForZSet().incrementScore(KEY_RANKING, videoIdStr, delta);

        // [Write-Back] 변경 감지 목록에 추가 -> 스케줄러가 이 Set을 뒤져서 DB에 반영함. (중복 방지를 위해 Set 사용)
        stringRedisTemplate.opsForSet().add(KEY_DIRTY_DATA, videoIdStr);
    }

    @Transactional(readOnly = true)
    public BookmarkSummaryResponse getBookmarkSummary(Long userId) {
        return BookmarkSummaryResponse.builder()
                .shortForm(buildPlaylist(userId, VideoType.SHORT))
                .longForm(buildPlaylist(userId, VideoType.LONG))
                .build();
    }

    private BookmarkPlaylistResponse buildPlaylist(Long userId, VideoType videoType) {
        List<String> thumbnailList = bookmarkRepository.findThumbnailByUserIdAndVideoType(userId, videoType, PageRequest.of(0, 4));
        long totalCount = bookmarkRepository.countByUserIdAndVideoType(userId, videoType);
        return BookmarkPlaylistResponse.builder()
                .totalCount(totalCount)
                .thumbnails(thumbnailList)
                .build();
    }

    @Transactional(readOnly = true)
    public BookmarkPageResponse getBookmarkList(Long userId, VideoType videoType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Slice<Bookmark> slice = bookmarkRepository.findByUserIdAndVideoType(userId, videoType, pageable);

        boolean hasMore = slice.hasNext();
        List<Bookmark> bookmarks = slice.getContent();

        Set<Long> uploaderIds = bookmarks.stream()
                .map(b -> b.getVideoMetadata().getUserId())
                .collect(Collectors.toSet());

        Map<Long, String> uploaderNameMap = userRepository.findAllById(uploaderIds).stream()
                .collect(Collectors.toMap(User::getId, User::getNickname));

        List<Long> videoMetadataIds = bookmarks.stream()
                .map(b -> b.getVideoMetadata().getId())
                .collect(Collectors.toList());

        List<WatchHistory> watchHistories = watchHistoryRepository.findByUserIdAndVideoMetadataIdIn(userId, videoMetadataIds);
        Map<Long, Double> watchProgressMap = watchHistories.stream()
                .collect(Collectors.toMap(
                        wh -> wh.getVideoMetadata().getId(),
                        wh -> calculateWatchProgressPercent(wh.getLastPosition(), wh.getVideoMetadata().getDuration())
                ));

        List<BookmarkListResponse> items = bookmarks.stream()
                .map(b -> {
                    VideoMetadata vm = b.getVideoMetadata();
                    return BookmarkListResponse.builder()
                            .videoId(String.valueOf(vm.getVideoId()))
                            .title(vm.getTitle())
                            .thumbnailUrl(vm.getThumbnailUrl())
                            .viewCount(vm.getViewCount())
                            .uploaderName(uploaderNameMap.getOrDefault(vm.getUserId(), ""))
                            .watchProgressPercent(watchProgressMap.getOrDefault(vm.getId(), 0.0))
                            .duration(vm.getDuration())
                            .uploadDate(vm.getCreatedAt())
                            .build();
                }).collect(Collectors.toList());

        return BookmarkPageResponse.builder()
                .items(items)
                .hasMore(hasMore)
                .build();
    }

    private double calculateWatchProgressPercent(Integer lastPosition, Integer duration) {
        if (duration == null || duration <= 0 || lastPosition == null) return 0.0;
        return Math.min(100.0, (double) lastPosition / duration * 100);
    }
}
