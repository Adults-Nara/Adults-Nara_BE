package com.ott.core.modules.preference.listener;

import com.ott.core.modules.preference.event.InteractionEvent;
import com.ott.core.modules.preference.event.VideoWatchedEvent;
import com.ott.core.modules.preference.service.UserPreferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserPreferenceEventListener {

    private final UserPreferenceService userPreferenceService;

    /**
     * @Async: 별도의 스레드에서 백그라운드로 실행해라! (사용자 응답 지연 방지)
     * @TransactionalEventListener: 일반 @EventListener보다 안전합니다.
     * 좋아요 DB 저장이 "완벽하게 성공(Commit)" 했을 때만 이 로직을 실행하라는 뜻입니다.
     */
    @Async("watchHistoryTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleVideoWatchedEvent(VideoWatchedEvent event) {
        log.info("[Event] 시청 기록 감지 - User: {}, Video: {}", event.userId(), event.videoId());

        userPreferenceService.reflectWatchScore(
                event.userId(),
                event.videoId(),
                event.watchSeconds(),
                event.isCompleted()
        );
    }

    @Async("watchHistoryTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleInteractionEvent(InteractionEvent event) {

        log.info("[Event Received] 인터랙션 변경 감지 - userId: {}, videoId: {}, old: {}, new: {}",
                event.userId(), event.videoId(), event.oldType(), event.newType());

        userPreferenceService.reflectInteractionScore(
                event.userId(),
                event.videoId(),
                event.oldType(),
                event.newType()
        );
    }
}
