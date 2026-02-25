package com.ott.media.modules.transcode.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@Slf4j
public class KafkaRetryConfig {

    @Bean
    public DefaultErrorHandler defaultErrorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {
        // 최종 실패 시 DLT로 보냄: 기본은 원본토픽 + ".DLT"
        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(kafkaTemplate);

        // 재시도: (간격 10초) x (재시도 3회)
        // FixedBackOff(intervalMs, maxAttempts)
        FixedBackOff backOff = new FixedBackOff(10_000L, 3L);

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);

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
    }}
