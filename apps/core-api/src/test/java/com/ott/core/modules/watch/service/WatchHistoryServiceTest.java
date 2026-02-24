package com.ott.core.modules.watch.service;

import com.ott.core.modules.video.service.SignedCookieProcessor;
import com.ott.core.modules.watch.dto.response.WatchHistoryResponse;
import com.ott.core.modules.watch.repository.WatchHistoryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class WatchHistoryServiceTest {

    @MockitoBean private SignedCookieProcessor signedCookieProcessor;
    @MockitoBean private software.amazon.awssdk.services.s3.S3Client s3Client;
    @MockitoBean private software.amazon.awssdk.services.s3.presigner.S3Presigner s3Presigner;

    @Autowired private RedisTemplate<String, Object> redisTemplate;
    @Autowired private WatchHistoryRedisService watchHistoryRedisService;
    @Autowired private WatchHistoryService watchHistoryService;
    @MockitoSpyBean private WatchHistoryRepository watchHistoryRepository;

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
    @DisplayName("Redis 캐시에 시청 이력 존재 시 DB 조회 없이 바로 반환")
    void 레디스_시청이력_존재하면_DB_조회X() {
        watchHistoryRedisService.saveWatchHistory(USER_ID, VIDEO_METADATA_ID, 100, 300);

        WatchHistoryResponse result = watchHistoryService.getWatchHistory(USER_ID, VIDEO_METADATA_ID);

        assertThat(result.getLastPosition()).isEqualTo(100);
        assertThat(result.getDuration()).isEqualTo(300);

        verify(watchHistoryRepository, never()).findByUserIdAndVideoMetadataId(any(), any());
    }
}