package com.ott.core.modules.video.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ott.common.outbox.entity.OutboxEvent;
import com.ott.common.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoTranscodeEventListener {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleTranscodeRequest(VideoTranscodeRequestedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = OutboxEvent.create(
                    "Video",
                    String.valueOf(event.videoId()),
                    "VideoTranscodeRequested",
                    "video-transcode-requested",
                    payload
            );

            outboxEventRepository.save(outboxEvent);

            log.info("[outbox] Outbox 이벤트 저장: videoId={}", event.videoId());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Outbox 이벤트 직렬화 실패", e);
        }
    }
}
