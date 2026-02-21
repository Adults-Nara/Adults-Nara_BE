package com.ott.core.modules.video.event;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class VideoTranscodeEventListener {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public VideoTranscodeEventListener(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTranscodeRequest(VideoTranscodeRequestedEvent event) {
        kafkaTemplate.send("video-transcode-requested", event);
    }
}
