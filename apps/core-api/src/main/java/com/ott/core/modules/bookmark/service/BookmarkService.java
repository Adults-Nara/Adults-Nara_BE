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

        Boolean hasKey = stringRedisTemplate.opsForHash().hasKey(KEY_VIDEO_COUNT, videoIdStr);
        if (Boolean.FALSE.equals(hasKey)) {
            long dbRealCount = bookmarkRepository.countByVideoMetadata_VideoId(videoId);
            stringRedisTemplate.opsForHash().put(KEY_VIDEO_COUNT, videoIdStr, String.valueOf(dbRealCount));
            log.info("[Redis Cache] 비어있는 캐시 초기화 세팅 완료 - videoId: {}, 카운트: {}", videoIdStr, dbRealCount);
        }

        // 증감 연산을 수행하고 그 결과값(최종 카운트)을 리턴받음
        Long currentCount = stringRedisTemplate.opsForHash().increment(KEY_VIDEO_COUNT, videoIdStr, delta);

        if (currentCount != null && currentCount < 0) {
            log.error("🚨 [Redis 오염 감지] 비디오 {}의 카운트가 {}이 되었습니다. DB 기준으로 즉시 강제 동기화합니다.", videoIdStr, currentCount);

            // 즉시 DB에서 진짜 숫자 검증
            currentCount = bookmarkRepository.countByVideoMetadata_VideoId(videoId);

            // 오염된 데이터를 찢어버리고 진짜 숫자로 덮어쓰기
            stringRedisTemplate.opsForHash().put(KEY_VIDEO_COUNT, videoIdStr, String.valueOf(currentCount));
            log.info("[Redis 복구 완료] 비디오 {} 카운트를 {}으로 덮어씌웠습니다.", videoIdStr, currentCount);
        }

        // 4. 안전한 최신 카운트로 랭킹(ZSet) 덮어쓰기 (increment 대신 add 사용)
        stringRedisTemplate.opsForZSet().add(KEY_RANKING, videoIdStr, currentCount != null ? currentCount.doubleValue() : 0.0);

        // 5. 스케줄러 동기화 큐 적재
        stringRedisTemplate.opsForSet().add(KEY_DIRTY_DATA, videoIdStr);
    }
}
