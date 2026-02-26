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

    @Scheduled(fixedDelay = 600000)
    public void runSync() {
        log.info("========== [Scheduler] 조회수, 반응 DB 동기화 시작 ==========");

        try {
            syncService.syncAllStats();
        } catch (Exception e) {
            log.error("[Scheduler] 동기화 스케줄러 실행 중 치명적인 에러 발생: {}", e.getMessage(), e);
        }

        log.info("========== [Scheduler] 조회수, 반응 DB 동기화 종료 ==========");
    }
}