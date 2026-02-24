package com.ott.core.modules.interaction.scheduler;

import com.ott.common.error.ErrorCode;
import com.ott.core.modules.video.repository.VideoMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoInteractionScheduler {

    private final VideoMetadataRepository videoMetadataRepository;
    private final StringRedisTemplate stringRedisTemplate;

    // 북마크 Redis Keys
    private static final String KEY_BOOKMARK_COUNT = "video:count:bookmark";
    private static final String KEY_DIRTY_BOOKMARK = "video:dirty:bookmark";

    @Scheduled(fixedDelay = 600000) // 10분마다 실행
    @Transactional
    public void syncBookmarkStats() {
        log.info("[Scheduler] 비디오 북마크 통계 DB 동기화 시작");

        // 변경된 적이 있는 비디오 ID 목록을 최대 1000개씩 꺼내옴
        List<String> dirtyVideoIds = stringRedisTemplate.opsForSet().pop(KEY_DIRTY_BOOKMARK, 1000);

        if (dirtyVideoIds == null || dirtyVideoIds.isEmpty()) {
            return;
        }

        for (String videoIdStr : dirtyVideoIds) {
            try {
                Long videoId = Long.valueOf(videoIdStr);

                // Redis에서 최신 카운트 조회
                Object countObj = stringRedisTemplate.opsForHash().get(KEY_BOOKMARK_COUNT, videoIdStr);
                if (countObj == null) continue;

                int latestCount = Integer.parseInt(countObj.toString());

                // DB 업데이트 (VideoMetadataRepository에 updateBookmarkCount 메서드 필수)
                videoMetadataRepository.updateBookmarkCount(videoId, latestCount);

            } catch (NumberFormatException e) {
                // Redis 값이 숫자가 아닐 때 (파싱 에러)
                log.error("[{}] {} (videoId: {})",
                        ErrorCode.REDIS_DATA_PARSING_ERROR.getCode(),
                        ErrorCode.REDIS_DATA_PARSING_ERROR.getMessage(),
                        videoIdStr);
            }
            catch (DataAccessException e) {
                // DB가 잠깐 죽었거나 커넥션 에러 났을 때 (DB 동기화 에러)
                log.error("[{}] {} (videoId: {})",
                        ErrorCode.DB_SYNC_ERROR.getCode(),
                        ErrorCode.DB_SYNC_ERROR.getMessage(),
                        videoIdStr);
                // 유실 방지를 위해 다시 큐에 넣음
                stringRedisTemplate.opsForSet().add(KEY_DIRTY_BOOKMARK, videoIdStr);
            }
            catch (Exception e) {
                // 그 외 알 수 없는 에러
                log.error("[Scheduler] 알 수 없는 에러 발생 (videoId: {}): {}", videoIdStr, e.getMessage());
                stringRedisTemplate.opsForSet().add(KEY_DIRTY_BOOKMARK, videoIdStr);
            }
        }
        log.info("[Scheduler] 북마크 통계 {}건 동기화 완료", dirtyVideoIds.size());
    }
}