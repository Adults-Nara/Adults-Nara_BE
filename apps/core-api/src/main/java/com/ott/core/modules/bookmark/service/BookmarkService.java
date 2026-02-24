package com.ott.core.modules.bookmark.service;

import com.ott.common.error.BusinessException;
import com.ott.common.error.ErrorCode;
import com.ott.common.persistence.entity.Bookmark;
import com.ott.common.persistence.entity.User;
import com.ott.common.persistence.entity.VideoMetadata;
import com.ott.core.modules.bookmark.repository.BookmarkRepository;
import com.ott.core.modules.user.repository.UserRepository;
import com.ott.core.modules.video.repository.VideoMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;
    private final VideoMetadataRepository videoMetadataRepository;
    private final StringRedisTemplate stringRedisTemplate;

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

        if (existingBookmark.isPresent()) {
            // 이미 찜했으면 -> 취소
            bookmarkRepository.delete(existingBookmark.get());
            updateRedis(videoId, -1); // Redis: 점수 감소
        } else {
            // 없으면 -> 찜하기
            Bookmark newBookmark = new Bookmark(user, metadata);
            bookmarkRepository.save(newBookmark);
            updateRedis(videoId, 1); // Redis: 점수 증가
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
}
