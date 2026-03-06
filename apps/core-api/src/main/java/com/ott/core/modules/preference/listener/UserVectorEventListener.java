package com.ott.core.modules.preference.listener;

import com.ott.common.persistence.enums.InteractionType;
import com.ott.core.modules.preference.event.InteractionEvent;
import com.ott.core.modules.preference.event.VideoWatchedEvent;
import com.ott.core.modules.preference.service.UserVectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserVectorEventListener {

    private final UserVectorService userVectorService;

    @Async("watchHistoryTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleVideoWatchedEvent(VideoWatchedEvent event) {
        // 영상을 끝까지 봤거나, 60초 이상 시청했을 때만 취향 벡터에 반영
        if (event.isCompleted() || (event.watchSeconds() != null && event.watchSeconds() > 60)) {
            log.debug("[AI Vector] 유의미한 시청 기록 감지 (벡터 갱신 대상) - User: {}, Video: {}",
                    event.userId(), event.videoId());

            userVectorService.updateVectorFromWatch(event.userId(), event.videoId(), event.isCompleted());
        }
    }

    @Async("watchHistoryTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleInteractionEvent(InteractionEvent event) {
        // 새로운 인터랙션이 '좋아요' 또는 '왕따봉'일 경우에만 취향 벡터에 강력하게 반영
        if (event.newType() == InteractionType.LIKE || event.newType() == InteractionType.SUPERLIKE) {
            log.debug("[AI Vector] 긍정적 인터랙션 감지 (벡터 갱신 대상) - User: {}, Action: {}",
                    event.userId(), event.newType());

            userVectorService.updateVectorFromInteraction(event.userId(), event.videoId(), event.newType());
        }
    }
}