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

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoInteractionScheduler {

    private final VideoMetadataRepository videoMetadataRepository;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String KEY_BOOKMARK_COUNT = "video:count:bookmark";
    private static final String KEY_DIRTY_BOOKMARK = "video:dirty:bookmark";

    @Scheduled(fixedDelay = 600000)
    @Transactional
    public void syncBookmarkStats() {
        log.info("[Scheduler] 비디오 북마크 통계 DB 동기화 시작");

        // pop 대신 randomMembers 사용 (데이터를 삭제하지 않고 읽어오기만 함)
        // 주의: SMEMBERS는 데이터가 수십만 개일 때 Redis를 멈추게 할 수 있으므로, 1000개씩 끊어 읽는 randomMembers가 안전합니다.
        List<String> dirtyVideoIds = stringRedisTemplate.opsForSet().randomMembers(KEY_DIRTY_BOOKMARK, 1000L);

        if (dirtyVideoIds == null || dirtyVideoIds.isEmpty()) {
            return;
        }

        // 성공적으로 DB에 반영된 ID들을 모을 리스트
        List<String> successIds = new ArrayList<>();

        for (String videoIdStr : dirtyVideoIds) {
            try {
                Long videoId = Long.valueOf(videoIdStr);

                Object countObj = stringRedisTemplate.opsForHash().get(KEY_BOOKMARK_COUNT, videoIdStr);
                if (countObj != null) {
                    int latestCount = Integer.parseInt(countObj.toString());
                    videoMetadataRepository.updateBookmarkCount(videoId, latestCount); // DB 업데이트
                }

                // 에러 없이 여기까지 도달했다면 '성공 목록'에 추가
                successIds.add(videoIdStr);

            } catch (NumberFormatException e) {
                // ErrorCode 활용: 파싱 에러
                log.error("[{}] {} (videoId: {}). 원인: {}",
                        ErrorCode.REDIS_DATA_PARSING_ERROR.getCode(),
                        ErrorCode.REDIS_DATA_PARSING_ERROR.getMessage(),
                        videoIdStr, e.getMessage());
                // 파싱 에러는 재시도해도 실패하므로 무한 루프 방지를 위해 성공 목록에 넣어 지워지게 합니다.
                successIds.add(videoIdStr);
            } catch (DataAccessException e) {
                // ErrorCode 활용: DB 동기화 실패
                log.error("[{}] {} (videoId: {})",
                        ErrorCode.DB_SYNC_ERROR.getCode(),
                        ErrorCode.DB_SYNC_ERROR.getMessage(),
                        videoIdStr);
                // DB 예외 시 성공 목록에 넣지 않음 -> Redis에 그대로 남아있어 다음 스케줄러가 다시 처리함
            } catch (Exception e) {
                log.error("[Scheduler] 알 수 없는 에러 발생 (videoId: {}): {}", videoIdStr, e.getMessage(), e);
            }
        }

        //처리에 성공한 ID들만 Redis Set에서 최종 삭제 (SREM)
        if (!successIds.isEmpty()) {
            stringRedisTemplate.opsForSet().remove(KEY_DIRTY_BOOKMARK, successIds.toArray());
            log.info("[Scheduler] 북마크 통계 {}건 동기화 및 Redis 캐시 정리 완료", successIds.size());
        }
    }
}