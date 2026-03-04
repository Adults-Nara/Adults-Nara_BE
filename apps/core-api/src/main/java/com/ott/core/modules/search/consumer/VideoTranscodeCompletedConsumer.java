package com.ott.core.modules.search.consumer;

import com.ott.core.modules.search.event.VideoIndexRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
@Slf4j
@Component
@RequiredArgsConstructor
public class VideoTranscodeCompletedConsumer {

    private final ApplicationEventPublisher eventPublisher;

    @KafkaListener(
            topics = "video-transcode-completed",
            groupId = "search-transcode-consumer-group",
            properties = {
                    "spring.json.use.type.headers=false",
                    "spring.json.value.default.type=com.ott.core.modules.search.consumer.VideoTranscodeCompletedEvent"
            }
    )
    public void onMessage(VideoTranscodeCompletedEvent event) {
        System.out.println("트랜스코딩 완료 이벤트 수신: videoId: " + event.videoId());

        eventPublisher.publishEvent(new VideoIndexRequestedEvent(event.videoId()));
    }
}
