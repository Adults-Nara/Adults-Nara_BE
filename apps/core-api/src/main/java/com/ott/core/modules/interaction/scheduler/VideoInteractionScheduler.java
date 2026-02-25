package com.ott.core.modules.interaction.scheduler;

import com.ott.core.modules.interaction.service.InteractionSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoInteractionScheduler {

    private final InteractionSyncService syncService;

    private static final List<String> SYNC_TARGETS = List.of("bookmark", "like", "dislike", "superlike");

    @Scheduled(fixedDelay = 600000)
    public void runSync() {
        log.info("========== [Scheduler] 비디오 통계 DB 동기화 시작 ==========");

        // 스케줄러가 각 타입별로 트랜잭션 메서드를 개별 호출!
        for (String targetType : SYNC_TARGETS) {
            try {
                syncService.syncForType(targetType);
            } catch (Exception e) {
                log.error("[Scheduler] {} 동기화 중 치명적 에러 발생: {}", targetType, e.getMessage());
            }
        }

        log.info("========== [Scheduler] 비디오 통계 DB 동기화 종료 ==========");
    }
}