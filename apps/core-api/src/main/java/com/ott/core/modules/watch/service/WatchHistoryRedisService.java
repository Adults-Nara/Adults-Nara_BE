package com.ott.core.modules.watch.service;

import com.ott.core.modules.watch.dto.WatchHistoryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class WatchHistoryRedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String WATCH_HISTORY_PREFIX = "watch:";
    private static final String RATE_LIMIT_PREFIX = "rate:";

    // TTL 설정
    private static final long WATCH_HISTORY_TTL_HOURS = 12; // 12시간
    private static final long RATE_LIMIT_TTL_SECONDS = 60; // 1분

    /**
     * 시청 이력 조회 (Redis)
     */
    public WatchHistoryDto getWatchHistory(Long userId, Long videoMetadataId) {
        String key = generateWatchKey(userId, videoMetadataId);
        Object value = redisTemplate.opsForValue().get(key);

        if (value instanceof WatchHistoryDto) {
            log.debug("[Redis] Hit - userId: {}, videoMetadataId: {}", userId, videoMetadataId);
            return (WatchHistoryDto) value;
        }

        log.debug("[Redis] Miss - userId: {}, videoMetadataId: {}", userId, videoMetadataId);
        return null;
    }

    /**
     * 시청 이력 저장 (Redis)
     */
    public void saveWatchHistory(Long userId, Long videoMetadataId, Integer lastPosition, Integer duration) {
        String key = generateWatchKey(userId, videoMetadataId);

        WatchHistoryDto watchHistory = WatchHistoryDto.builder()
                .userId(userId)
                .videoMetadataId(videoMetadataId)
                .lastPosition(lastPosition)
                .duration(duration)
                .build();

        redisTemplate.opsForValue().set(key, watchHistory, WATCH_HISTORY_TTL_HOURS, TimeUnit.HOURS);
        log.info("[Redis] Updated - userId: {}, videoMetadataId: {}, lastPosition: {}, duration: {}",
                userId, videoMetadataId, lastPosition, duration);
    }

    /**
     * Rate Limit 체크 (1분 주기로 DB 저장)
     * @return true: DB 저장 가능, false: DB 저장 스킵
     */
    public boolean checkRateLimit(Long userId, Long videoMetadataId) {
        String key = generateRateLimitKey(userId, videoMetadataId);

        // key가 존재하면 False, 존재하지 않으면 True 반환
        Boolean isFirstRequest = redisTemplate.opsForValue().setIfAbsent(key, "1", RATE_LIMIT_TTL_SECONDS, TimeUnit.SECONDS);

        return Boolean.TRUE.equals(isFirstRequest);
    }

    public void deleteWatchHistory(Long userId, Long videoMetadataId) {
        String key = generateWatchKey(userId, videoMetadataId);
        redisTemplate.delete(key);
    }

    private String generateWatchKey(Long userId, Long videoMetaDataId) {
        return WATCH_HISTORY_PREFIX + userId + ":" + videoMetaDataId;
    }

    private String generateRateLimitKey(Long userId, Long videoMetaDataId) {
        return RATE_LIMIT_PREFIX + userId + ":" + videoMetaDataId;
    }
}
