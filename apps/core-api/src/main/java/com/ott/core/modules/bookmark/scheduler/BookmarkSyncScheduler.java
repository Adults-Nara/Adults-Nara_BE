package com.ott.core.modules.bookmark.scheduler;

import com.ott.core.modules.bookmark.service.BookmarkSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookmarkSyncScheduler {

    private final BookmarkSyncService bookmarkSyncService;

    /**
     * [Step 2] Redis에 저장된 각 비디오의 북마크 카운트가 VideoMetadata DB에 동기화됨
     * - 10분(600,000ms)마다 실행됩니다.
     */
    @Scheduled(fixedDelay = 600000)
    public void runBookmarkSync() {
        log.info("========== [Scheduler] 북마크 카운트 DB 동기화 시작 ==========");

        try {
            // 비즈니스 로직 호출
            bookmarkSyncService.syncBookmarkCounts();
        } catch (Exception e) {
            log.error("[Scheduler] 북마크 동기화 스케줄러 실행 중 치명적 에러 발생: {}", e.getMessage(), e);
        }

        log.info("========== [Scheduler] 북마크 카운트 DB 동기화 종료 ==========");
    }
}
