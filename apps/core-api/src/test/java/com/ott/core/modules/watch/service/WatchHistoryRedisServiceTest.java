package com.ott.core.modules.watch.service;

import com.ott.core.modules.video.service.SignedCookieProcessor;
import com.ott.core.modules.watch.dto.WatchHistoryDto;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
class WatchHistoryRedisServiceTest {

    @MockitoBean private SignedCookieProcessor signedCookieProcessor;
    @Autowired private WatchHistoryRedisService watchHistoryRedisService;
    @Autowired private RedisTemplate<String, Object> redisTemplate;

    private static final Long USER_ID = 1L;
    private static final Long VIDEO_METADATA_ID = 100L;

    private String watchKey() {
        return "watch:" + USER_ID + ":" + VIDEO_METADATA_ID;
    }

    private String rateKey() {
        return "rate:" + USER_ID + ":" + VIDEO_METADATA_ID;
    }

    @AfterEach
    void cleanUp() {
        redisTemplate.delete(watchKey());
        redisTemplate.delete(rateKey());
    }

    @Test
    @DisplayName("시청 이력 저장 후 조회 테스트")
    void saveAndGetWatchHistory() {
        watchHistoryRedisService.saveWatchHistory(USER_ID, VIDEO_METADATA_ID, 100, 300);
        WatchHistoryDto result = watchHistoryRedisService.getWatchHistory(USER_ID, VIDEO_METADATA_ID);

        assertThat(result.getLastPosition()).isEqualTo(100);
        assertThat(result.getDuration()).isEqualTo(300);
    }

    @Test
    @DisplayName("RateLimit 테스트")
    void rateLimitTest() {
        boolean firstResult = watchHistoryRedisService.checkRateLimit(USER_ID, VIDEO_METADATA_ID);
        boolean secondResult = watchHistoryRedisService.checkRateLimit(USER_ID, VIDEO_METADATA_ID);

        assertThat(firstResult).isTrue();
        assertThat(secondResult).isFalse();
    }

    @Test
    @DisplayName("WATCH_HISTORY_TTL 동작 확인")
    void ttlTest() {
        watchHistoryRedisService.saveWatchHistory(USER_ID, VIDEO_METADATA_ID, 10, 100);
        Long ttl = redisTemplate.getExpire(watchKey(), TimeUnit.HOURS);

        assertThat(ttl).isNotNull();
        assertThat(ttl).isBetween(11L, 12L);
    }

    @Test
    @DisplayName("삭제 테스트")
    void deleteTest() {
        watchHistoryRedisService.saveWatchHistory(USER_ID, VIDEO_METADATA_ID, 10, 100);
        watchHistoryRedisService.deleteWatchHistory(USER_ID, VIDEO_METADATA_ID);

        Object value = redisTemplate.opsForValue().get(watchKey());
        assertThat(value).isNull();
    }

    @Test
    @DisplayName("시청 이력 업데이트 테스트")
    void saveWatchHistory_overwrite() {
        watchHistoryRedisService.saveWatchHistory(USER_ID, VIDEO_METADATA_ID, 10, 300);
        watchHistoryRedisService.saveWatchHistory(USER_ID, VIDEO_METADATA_ID, 30, 300);

        WatchHistoryDto result = watchHistoryRedisService.getWatchHistory(USER_ID, VIDEO_METADATA_ID);
        assertThat(result.getLastPosition()).isEqualTo(30);
    }
}