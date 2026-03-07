package com.ott.core.modules.ai.consumer;

import com.ott.core.modules.ai.dto.VideoAiAnalysisCompletedEvent;
import com.ott.core.modules.ai.service.AiAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiAnalysisCompletedConsumer {

    private final AiAnalysisService aiAnalysisService;

    @KafkaListener(topics = "video-ai-analysis-completed", groupId = "core-api-ai-consumer-group", properties = {
            "spring.json.use.type.headers=false",
            "spring.json.value.default.type=com.ott.core.modules.ai.dto.VideoAiAnalysisCompletedEvent"
    })
    public void onMessage(VideoAiAnalysisCompletedEvent event) {
        log.info("AI 분석 완료 이벤트 수신: videoId={}", event.videoId());

        if ("COMPLETED".equals(event.status())) {
            aiAnalysisService.processAnalysisResult(event);
        } else {
            log.error("AI 분석 실패 수신: videoId={}, error={}", event.videoId(), event.error());
        }
    }
}
