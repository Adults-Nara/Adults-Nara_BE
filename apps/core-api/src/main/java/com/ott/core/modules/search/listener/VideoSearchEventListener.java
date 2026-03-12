package com.ott.core.modules.search.listener;

import com.ott.core.modules.search.event.VideoIndexRequestedEvent;
import com.ott.core.modules.search.service.VideoSearchSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoSearchEventListener {

    private final VideoSearchSyncService videoSearchSyncService;

    /**
     * 비동기로 이벤트를 받아서 동기화 서비스로 던져주기만 합니다.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleVideoIndexRequest(VideoIndexRequestedEvent event) {
        // 복잡한 껍데기(Proxy) 충돌 없이, 서비스 메서드를 깨끗하게 호출!
        videoSearchSyncService.syncToElasticsearch(event.videoId());
    }
}