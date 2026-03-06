package com.ott.core.modules.interaction.service;

import com.ott.core.modules.interaction.repository.InteractionRepository;
import com.ott.core.modules.video.repository.VideoMetadataRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

@Slf4j
@Service
public class InteractionSyncService {
    private final StringRedisTemplate stringRedisTemplate;
    private final InteractionRepository interactionRepository;

    // OCP(개방-폐쇄 원칙) 준수: 좋아요, 싫어요 등 타입별 DB 업데이트 메서드 매핑
    private final Map<String, BiConsumer<Long, Integer>> syncActionMap;

    public InteractionSyncService(
            StringRedisTemplate stringRedisTemplate,
            VideoMetadataRepository videoMetadataRepository,
            InteractionRepository interactionRepository) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.interactionRepository = interactionRepository;

        this.syncActionMap = Map.of(
                "like", videoMetadataRepository::updateLikeCount,
                "dislike", videoMetadataRepository::updateDislikeCount,
                "superlike", videoMetadataRepository::updateSuperLikeCount
        );
    }

    public void syncAllStats() {
        syncActionMap.keySet().forEach(this::processSyncForType);
    }

    protected void processSyncForType(String targetType) {
        String dirtyKey = "video:dirty:" + targetType;
        String processingKey = "video:processing:" + targetType;
        String countKey = "video:count:" + targetType;

        // 1. [장애 복구] 이전 작업 중 서버가 뻗었다면 다시 합침
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(processingKey))) {
            stringRedisTemplate.opsForSet().unionAndStore(dirtyKey, processingKey, dirtyKey);
            stringRedisTemplate.delete(processingKey);
        }

        if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(dirtyKey))) {
            return; // 동기화할 데이터가 없으면 조용히 종료
        }

        // 2. [동시성 방어] 대기열을 작업장(processing)으로 원자적 이동
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

        log.info("[SyncService] {} 카운트 DB 동기화 시작 (대상: {}건)", targetType, videoIdsToProcess.size());
        int successCount = 0;

        for (String videoIdStr : videoIdsToProcess) {
            try {
                Long videoId = Long.valueOf(videoIdStr);

                // 3. Redis Hash에서 현재 이 비디오의 인터랙션(좋아요 등) 카운트를 가져옵니다. (ZSet이 아님!)
                Object countObj = stringRedisTemplate.opsForHash().get(countKey, videoIdStr);

                if (countObj != null) {
                    int latestCount = Integer.parseInt(countObj.toString());

                    // 4. Repository에 이미 @Transactional이 있으므로 안전하게 업데이트 됨
                    syncActionMap.get(targetType).accept(videoId, latestCount);
                    successCount++;
                }

            } catch (Exception e) {
                log.error("[SyncService] 비디오 {} {} 동기화 중 에러 발생: {}", videoIdStr, targetType, e.getMessage());
                // DLQ 대신 BookmarkSyncService처럼 실패 건을 dirty에 다시 넣어 다음 턴에 재시도
                stringRedisTemplate.opsForSet().add(dirtyKey, videoIdStr);
            }
        }

        // 5. 완료된 큐 삭제
        stringRedisTemplate.delete(processingKey);

        if (successCount > 0) {
            log.info("[SyncService] {} 카운트 {}건 DB 완벽 동기화 완료!", targetType, successCount);
        }
    }
}