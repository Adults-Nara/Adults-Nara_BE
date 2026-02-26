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
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;
    private final VideoMetadataRepository videoMetadataRepository;
    private final StringRedisTemplate stringRedisTemplate;

    // Redis Key 상수
    private static final String KEY_RANKING = "video:ranking"; // 실시간 차트 (ZSet)
    private static final String KEY_DIRTY = "video:dirty:bookmark"; // 스케줄러 대기열 (Set)

    @Transactional
    public void toggleBookmark(Long userId, Long videoId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        VideoMetadata metadata = videoMetadataRepository.findByVideoId(videoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VIDEO_NOT_FOUND));

        Optional<Bookmark> existingBookmark = bookmarkRepository.findByUserAndVideoMetadata(user, metadata);

        try {
            if (existingBookmark.isPresent()) {
                bookmarkRepository.delete(existingBookmark.get());
                bookmarkRepository.flush(); // 즉시 삭제 쿼리 실행
            } else {
                Bookmark newBookmark = new Bookmark(user, metadata);
                bookmarkRepository.save(newBookmark);
                bookmarkRepository.flush(); // 즉시 저장 쿼리 실행
            }

            // 1. DB 저장이 완료된 후, 실제 북마크 개수를 다시 셉니다. (가장 정확한 팩트 데이터)
            long realCount = bookmarkRepository.countByVideoId(videoId);
            String videoIdStr = String.valueOf(videoId);

            // 2. Redis ZSet 실시간 랭킹 차트 갱신
            stringRedisTemplate.opsForZSet().add(KEY_RANKING, videoIdStr, realCount);

            stringRedisTemplate.opsForSet().add(KEY_DIRTY, videoIdStr);

            log.info("[Bookmark] 비디오 {} 북마크 변경 완료. 현재 총 카운트: {} (스케줄러 대기열 적재 완료)", videoIdStr, realCount);

        } catch (DataIntegrityViolationException e) {
            log.warn("[Bookmark] 동시성 방어 (따닥 클릭 무시) - userId: {}, videoId: {}", userId, videoId);
        }
    }

    @Transactional(readOnly = true)
    public boolean isBookmarked(Long userId, Long videoId) {
        return bookmarkRepository.existsByUserIdAndVideoId(userId, videoId);
    }
}