package com.ott.core.modules.watch.service;

import com.ott.common.persistence.entity.WatchHistory;
import com.ott.common.util.IdGenerator;
import com.ott.core.modules.preference.event.VideoWatchedEvent;
import com.ott.core.modules.preference.service.UserPreferenceService;
import com.ott.core.modules.watch.dto.WatchHistoryDto;
import com.ott.core.modules.watch.dto.response.WatchHistoryResponse;
import com.ott.core.modules.watch.repository.WatchHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WatchHistoryService {

    private final WatchHistoryRepository watchHistoryRepository;
    private final WatchHistoryRedisService watchHistoryRedisService;
    private final WatchHistoryAsyncService watchHistoryAsyncService;
    private final UserPreferenceService userPreferenceService;
    private final ApplicationEventPublisher eventPublisher;
    /**
     *  시청 이력 조회
     */
    public WatchHistoryResponse getWatchHistory(Long userId, Long videoMetadataId) {

        WatchHistoryDto redisHistory = watchHistoryRedisService.getWatchHistory(userId, videoMetadataId);

        // Redis에 시청 이력이 존재하는지 확인
        if (redisHistory != null) {
            return WatchHistoryResponse.builder()
                    .videoMetadataId(String.valueOf(videoMetadataId))
                    .lastPosition(redisHistory.getLastPosition())
                    .duration(redisHistory.getDuration())
                    .build();
        }

        // DB에 시청 이력이 존재하는지 확인
        WatchHistory watchHistory = watchHistoryRepository.findByUserIdAndVideoMetadataId(userId, videoMetadataId).orElse(null);

        if (watchHistory != null) {
            // DB에 시청이력이 존재하면 Redis에 캐싱 및 반환
            watchHistoryRedisService.saveWatchHistory(
                    userId,
                    videoMetadataId,
                    watchHistory.getLastPosition(),
                    watchHistory.getVideoMetadata().getDuration()
            );

            return WatchHistoryResponse.builder()
                    .videoMetadataId(String.valueOf(videoMetadataId))
                    .lastPosition(watchHistory.getLastPosition())
                    .duration(watchHistory.getVideoMetadata().getDuration())
                    .build();
        } else {
            // DB에 시청이력이 존재하지 않으면 0초 반환
            return WatchHistoryResponse.builder()
                    .videoMetadataId(String.valueOf(videoMetadataId))
                    .lastPosition(0)
                    .duration(null)
                    .build();
        }
    }

    /**
     * 시청 위치 업데이트 (10초마다 호출)
     */
    public void updateWatchPosition(Long userId, Long videoMetadataId, Integer lastPosition, Integer duration) {

        // 도메인 로직을 사용하여 완주 여부 계산
        boolean isCompleted = WatchHistory.isVideoCompleted(lastPosition, duration);

        watchHistoryRedisService.saveWatchHistory(userId, videoMetadataId, lastPosition, duration);
        boolean canSaveToDb = watchHistoryRedisService.checkRateLimit(userId, videoMetadataId);
        if (canSaveToDb || isCompleted) {
            watchHistoryAsyncService.saveWatchHistoryToDb(userId, videoMetadataId, lastPosition, isCompleted);
        }
    }

    /**
     * 시청 종료 시 최종 위치 DB 저장 (Rate limit 무시)
     */
    @Transactional
    public void stopWatching(Long userId, Long videoMetadataId, Integer lastPosition, Integer duration) {

        // 종료 시점에 완주 여부 계산
        boolean isCompleted = WatchHistory.isVideoCompleted(lastPosition, duration);
        watchHistoryRepository.upsertWatchHistory(IdGenerator.generate(), userId, videoMetadataId, lastPosition, isCompleted, OffsetDateTime.now(ZoneOffset.UTC));
        userPreferenceService.reflectWatchScore(userId, videoMetadataId, lastPosition, isCompleted);
        watchHistoryRedisService.deleteWatchHistory(userId, videoMetadataId);
        eventPublisher.publishEvent(new VideoWatchedEvent(userId, videoMetadataId, lastPosition, isCompleted));
    }
}
