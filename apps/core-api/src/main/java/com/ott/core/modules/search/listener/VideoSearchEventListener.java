package com.ott.core.modules.search.listener;

import com.ott.core.modules.backoffice.event.VideoUpdatedEvent;
import com.ott.core.modules.backoffice.event.VideoVisibilityChangedEvent;
import com.ott.core.modules.search.event.VideoIndexDeletedEvent;
import com.ott.core.modules.search.event.VideoIndexRequestedEvent;
import com.ott.core.modules.search.service.VideoSearchSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoSearchEventListener {

    private final VideoSearchSyncService videoSearchSyncService;

    // 1. 단일 영상 수정 이벤트 (태그/제목 등)
    @Async("searchTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleVideoUpdated(VideoUpdatedEvent event) {
        log.info("[Search Listener] 비디오 업데이트 이벤트 수신: videoId={}, visible={}", event.videoId(), event.isVisible());
        if (event.isVisible()) {
            videoSearchSyncService.syncToElasticsearch(event.videoId()); // 덮어쓰기 (태그 등 갱신)
        } else {
            videoSearchSyncService.deleteFromElasticsearch(List.of(event.videoId())); // ES에서 삭제
        }
    }

    /**
     * 비동기로 이벤트를 받아서 동기화 서비스로 던져주기만 합니다.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleVideoIndexRequest(VideoIndexRequestedEvent event) {
        // 복잡한 껍데기(Proxy) 충돌 없이, 서비스 메서드를 깨끗하게 호출!
        videoSearchSyncService.syncToElasticsearch(event.videoId());
    }

    // 삭제 이벤트 수신 처리
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleVideoIndexDelete(VideoIndexDeletedEvent event) {
        log.info("[Search Listener] 비디오 삭제 이벤트 수신: videoId={}", event.videoIds());
        videoSearchSyncService.deleteFromElasticsearch(event.videoIds());
    }

    @Async("searchTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleVideoVisibilityChanged(VideoVisibilityChangedEvent event) {
        log.info("[Search Listener] 비디오 상태변경 이벤트 수신: videoIds={}, visible={}", event.videoIds(), event.isVisible());
        if (event.isVisible()) {
            for (Long videoId : event.videoIds()) {
                videoSearchSyncService.syncToElasticsearch(videoId);
            }
        } else {
            videoSearchSyncService.deleteFromElasticsearch(event.videoIds());
        }
    }
}