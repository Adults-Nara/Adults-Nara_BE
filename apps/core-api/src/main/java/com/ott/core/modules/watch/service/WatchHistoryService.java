package com.ott.core.modules.watch.service;

import com.ott.common.error.BusinessException;
import com.ott.common.error.ErrorCode;
import com.ott.common.persistence.entity.User;
import com.ott.common.persistence.entity.VideoMetadata;
import com.ott.common.persistence.entity.WatchHistory;
import com.ott.common.util.IdGenerator;
import com.ott.core.modules.preference.event.VideoWatchedEvent;
import com.ott.core.modules.preference.service.UserPreferenceService;
import com.ott.core.modules.user.repository.UserRepository;
import com.ott.core.modules.video.repository.VideoMetadataRepository;
import com.ott.core.modules.watch.dto.WatchHistoryDto;
import com.ott.core.modules.watch.dto.response.WatchHistoryItemResponse;
import com.ott.core.modules.watch.dto.response.WatchHistoryPageResponse;
import com.ott.core.modules.watch.dto.response.WatchHistoryResponse;
import com.ott.core.modules.watch.repository.WatchHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final VideoMetadataRepository videoMetadataRepository;
    private final UserRepository userRepository;

    /**
     *  시청 이력 조회
     */
    public WatchHistoryResponse getWatchHistory(Long userId, Long videoId) {

        WatchHistoryDto redisHistory = watchHistoryRedisService.getWatchHistory(userId, videoId);

        // Redis에 시청 이력이 존재하는지 확인
        if (redisHistory != null) {
            return WatchHistoryResponse.builder()
                    .videoId(String.valueOf(videoId))
                    .lastPosition(redisHistory.getLastPosition())
                    .duration(redisHistory.getDuration())
                    .build();
        }

        // DB에 시청 이력이 존재하는지 확인
        VideoMetadata videoMetadata = videoMetadataRepository.findByVideoIdAndDeleted(videoId, false).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        Long videoMetadataId = videoMetadata.getId();
        WatchHistory watchHistory = watchHistoryRepository.findByUserIdAndVideoMetadataId(userId, videoMetadataId).orElse(null);

        if (watchHistory != null) {
            // DB에 시청이력이 존재하면 Redis에 캐싱 및 반환
            watchHistoryRedisService.saveWatchHistory(
                    userId,
                    videoId,
                    watchHistory.getLastPosition(),
                    videoMetadata.getDuration()
            );

            return WatchHistoryResponse.builder()
                    .videoId(String.valueOf(videoId))
                    .lastPosition(watchHistory.getLastPosition())
                    .duration(videoMetadata.getDuration())
                    .build();
        } else {
            // DB에 시청이력이 존재하지 않으면 0초 반환
            return WatchHistoryResponse.builder()
                    .videoId(String.valueOf(videoId))
                    .lastPosition(0)
                    .duration(videoMetadata.getDuration())
                    .build();
        }
    }

    /**
     * 시청 위치 업데이트 (10초마다 호출)
     */
    public void updateWatchPosition(Long userId, Long videoId, Integer lastPosition, Integer duration) {

        // 도메인 로직을 사용하여 완주 여부 계산
        boolean isCompleted = WatchHistory.isVideoCompleted(lastPosition, duration);

        watchHistoryRedisService.saveWatchHistory(userId, videoId, lastPosition, duration);
        boolean canSaveToDb = watchHistoryRedisService.checkRateLimit(userId, videoId);

        VideoMetadata videoMetadata = videoMetadataRepository.findByVideoIdAndDeleted(videoId, false).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        Long videoMetadataId = videoMetadata.getId();
        if (canSaveToDb || isCompleted) {
            watchHistoryAsyncService.saveWatchHistoryToDb(userId, videoMetadataId, lastPosition, isCompleted);
        }
    }

    /**
     * 시청 종료 시 최종 위치 DB 저장 (Rate limit 무시)
     */
    @Transactional
    public void stopWatching(Long userId, Long videoId, Integer lastPosition, Integer duration) {

        // 종료 시점에 완주 여부 계산
        boolean isCompleted = WatchHistory.isVideoCompleted(lastPosition, duration);

        VideoMetadata videoMetadata = videoMetadataRepository.findByVideoIdAndDeleted(videoId, false).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        Long videoMetadataId = videoMetadata.getId();

        watchHistoryRepository.upsertWatchHistory(IdGenerator.generate(), userId, videoMetadataId, lastPosition, isCompleted, OffsetDateTime.now(ZoneOffset.UTC));
        userPreferenceService.reflectWatchScore(userId, videoMetadataId, lastPosition, isCompleted);
        watchHistoryRedisService.deleteWatchHistory(userId, videoId);
        eventPublisher.publishEvent(new VideoWatchedEvent(userId, videoMetadataId, lastPosition, isCompleted));
    }

    /**
     * 최근 3개월 시청 이력 조회 (캐러셀 / 바텀시트 공용)
     */
    public WatchHistoryPageResponse getRecentWatchHistory(long userId, int page, int size) {
        OffsetDateTime threeMonthsAgo = OffsetDateTime.now(ZoneOffset.UTC).minusMonths(3);

        long offset = (long) page * size;
        List<WatchHistory> histories = watchHistoryRepository.findRecentHistory(userId, threeMonthsAgo, size + 1, offset);

        boolean hasMore = histories.size() > size;
        List<WatchHistory> pageItems = hasMore ? histories.subList(0, size) : histories;

        Set<Long> uploaderIds = pageItems.stream()
                .map(wh -> wh.getVideoMetadata().getUserId())
                .collect(Collectors.toSet());

        Map<Long, String> uploaderNameMap = userRepository.findAllById(uploaderIds).stream()
                .collect(Collectors.toMap(User::getId, User::getNickname));

        List<WatchHistoryItemResponse> items = pageItems.stream()
                .map(wh -> {
                    VideoMetadata vm = wh.getVideoMetadata();
                    return WatchHistoryItemResponse.builder()
                            .videoId(String.valueOf(vm.getVideoId()))
                            .title(vm.getTitle())
                            .thumbnailUrl(vm.getThumbnailUrl())
                            .viewCount(vm.getViewCount())
                            .uploaderName(uploaderNameMap.getOrDefault(vm.getUserId(), ""))
                            .watchProgressPercent(calculateWatchProgressPercent(wh.getLastPosition(), vm.getDuration()))
                            .watchedAt(wh.getUpdatedAt())
                            .build();
                })
                .collect(Collectors.toList());

        return WatchHistoryPageResponse.builder()
                .items(items)
                .hasMore(hasMore)
                .build();
    }

    private double calculateWatchProgressPercent(Integer lastPosition, Integer duration) {
        if (duration == null || duration <= 0 || lastPosition == null) {
            return 0.0;
        }
        return Math.min(100.0, (double) lastPosition / duration * 100);
    }

}
