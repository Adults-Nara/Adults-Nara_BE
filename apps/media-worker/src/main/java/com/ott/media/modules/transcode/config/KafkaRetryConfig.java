package com.ott.media.modules.transcode.config;

import com.ott.media.modules.transcode.dto.VideoTranscodeRequestedEvent;
import com.ott.media.modules.transcode.service.VideoUpdater;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@Slf4j
public class KafkaRetryConfig {
    private final VideoUpdater videoUpdater;

    public KafkaRetryConfig(VideoUpdater videoUpdater) {
        this.videoUpdater = videoUpdater;
    }

    @Bean
    public DefaultErrorHandler defaultErrorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {
        // 최종 실패 시 DLT로 보냄: 기본은 원본토픽 + ".DLT"
        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(kafkaTemplate, (record, ex) -> {
                    // DLT 발행 직전에 FAILED 상태 확정
                    try {
                        Object v = record.value();
                        if (v instanceof VideoTranscodeRequestedEvent evt) {
                            log.info("[Fail] dlt로 전송 videoId = {}", evt.videoId());
                            videoUpdater.updateFail(evt.videoId());
                        }
                    } catch (Exception failEx) {
                        // 여기서 예외 터지면 DLT 발행까지 막을 수 있으니
                        // 반드시 DLT 발행은 진행시키는 게 운영상 안전함
                    }
                    return new TopicPartition(record.topic() + ".DLT", record.partition());
                });

        // 재시도: (간격 10초) x (재시도 3회)
        // FixedBackOff(intervalMs, maxAttempts)
        FixedBackOff backOff = new FixedBackOff(10_000L, 3L);

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        // recover(DLT 보냄) 후 오프셋을 커밋하도록
        handler.setCommitRecovered(true);
        // 처리(에러핸들링) 후 ack 하도록 (대부분 기본 true)
        handler.setAckAfterHandle(true);

        // 재시도 "하면 안 되는" 예외는 제외 가능(예: 데이터 포맷 문제는 재시도 의미 없음)
        // handler.addNotRetryableExceptions(IllegalArgumentException.class);

        // 로깅 훅(선택)
        handler.setRetryListeners((record, ex, deliveryAttempt) -> {
            // deliveryAttempt: 1부터 증가
            // record: ConsumerRecord<?, ?>
            log.info("retry attempt={} topic={} offset={} key={}",
                    deliveryAttempt, record.topic(), record.offset(), record.key());
        });

        return handler;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaListenerContainerFactory(
            ConsumerFactory<Object, Object> consumerFactory,
            DefaultErrorHandler defaultErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);

        // 직접 만든 DefaultErrorHandler를 컨테이너에 연결
        factory.setCommonErrorHandler(defaultErrorHandler);

        // 수동 ack 쓰지 말고, 프레임워크에게 커밋을 맡기는 게 재시도/DLT에서 훨씬 안전
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);

        return factory;
    }
}
