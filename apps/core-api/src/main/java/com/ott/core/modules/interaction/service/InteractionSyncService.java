package com.ott.core.modules.interaction.service;

import com.ott.common.error.ErrorCode;
import com.ott.core.modules.video.repository.VideoMetadataRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

@Slf4j
@Service
public class InteractionSyncService {

    private final StringRedisTemplate stringRedisTemplate;
    private final Map<String, BiConsumer<Long, Integer>> syncActionMap;

    public InteractionSyncService(StringRedisTemplate stringRedisTemplate, VideoMetadataRepository videoMetadataRepository) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.syncActionMap = Map.of(
                "bookmark", videoMetadataRepository::updateBookmarkCount,
                "like", videoMetadataRepository::updateLikeCount,
                "dislike", videoMetadataRepository::updateDislikeCount,
                "superlike", videoMetadataRepository::updateSuperLikeCount
        );
    }

    /**
     * Scheduler에서 단건씩 호출하도록 public
     */
    @Transactional
    public void syncForType(String targetType) {
        String dirtyKey = "video:dirty:" + targetType;
        String processingKey = "video:processing:" + targetType;
        String countKey = "video:count:" + targetType;

        // 1. 이전 작업 실패 감지 및 복구
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(processingKey))) {
            log.warn("[Scheduler] 이전 작업 실패 감지. 복구를 시작합니다. (Type: {})", targetType);
            stringRedisTemplate.opsForSet().unionAndStore(dirtyKey, processingKey, dirtyKey);
            stringRedisTemplate.delete(processingKey);
        }

        // 2. 동기화할 데이터가 없으면 즉시 종료
        if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(dirtyKey))) {
            return;
        }

        // 3. Key Rotation (실시간 요청과 분리)
        try {
            stringRedisTemplate.rename(dirtyKey, processingKey);
        } catch (Exception e) {
            return;
        }

        Set<String> videoIdsToProcess = stringRedisTemplate.opsForSet().members(processingKey);

        if (videoIdsToProcess == null || videoIdsToProcess.isEmpty()) {
            stringRedisTemplate.delete(processingKey);
            return;
        }

        int successCount = 0;

        // 4. DB 업데이트
        for (String videoIdStr : videoIdsToProcess) {
            try {
                Long videoId = Long.valueOf(videoIdStr);
                Object countObj = stringRedisTemplate.opsForHash().get(countKey, videoIdStr);

                if (countObj != null) {
                    int latestCount = Integer.parseInt(countObj.toString());

                    if (latestCount < 0) {
                        log.warn("[Scheduler] 비정상적인 마이너스 카운트 감지 (Type: {}, videoId:{}). 0으로 교정합니다.", targetType, videoId);
                        latestCount = 0;

                        stringRedisTemplate.opsForHash().put(countKey, videoIdStr, "0");

                        if("bookmark".equals(targetType)) {
                            Double rankingScore = stringRedisTemplate.opsForZSet().score("video:ranking", videoIdStr);
                            if (rankingScore !=null && rankingScore < 0){
                                stringRedisTemplate.opsForZSet().add("videl:ranking", videoIdStr, 0.0);
                            }
                        }
                    }
                    syncActionMap.get(targetType).accept(videoId, latestCount); // 정상적으로 트랜잭션 적용됨
                    successCount++;
                }
            } catch (NumberFormatException e) {
                log.error("[{}] 파싱 에러 (Type: {}, videoId: {})", ErrorCode.REDIS_DATA_PARSING_ERROR.getCode(), targetType, videoIdStr);
            } catch (DataAccessException e) {
                log.error("[{}] DB 업데이트 에러 (Type: {}, videoId: {})", ErrorCode.DB_SYNC_ERROR.getCode(), targetType, videoIdStr);
                stringRedisTemplate.opsForSet().add(dirtyKey, videoIdStr);
            } catch (Exception e) {
                log.error("[Scheduler] 알 수 없는 에러 (Type: {}, videoId: {}): {}", targetType, videoIdStr, e.getMessage());
            }
        }

        // 5. 완료 후 캐시 정리
        stringRedisTemplate.delete(processingKey);

        if (successCount > 0) {
            log.info("[Scheduler] {} 통계 {}건 DB 완벽 동기화 완료", targetType, successCount);
        }
    }
}