package com.ott.common.outbox.scheduler;

import com.ott.common.outbox.entity.OutboxEvent;
import com.ott.common.outbox.enums.OutboxStatus;
import com.ott.common.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final int MAX_RETRY = 3;

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> events = outboxEventRepository
                .findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

        for (OutboxEvent event : events) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getAggregateId(), event.getPayload())
                        .get(5, TimeUnit.SECONDS);

                event.markPublished();

                log.info("[outbox] 발행 성공: topic={}, aggregateId={}, eventType={}",
                        event.getTopic(), event.getAggregateId(), event.getEventType());
            } catch (Exception e) {
                event.incrementRetry();

                if (event.getRetryCount() >= MAX_RETRY) {
                    event.markFailed();
                    log.error("[outbox] 최대 재시도 초과, FAILED 처리: topic={}, aggregateId={}",
                            event.getTopic(), event.getAggregateId(), e);
                } else {
                    log.warn("[outbox] 발행 실패 (retry={}): topic={}, aggregateId={}",
                            event.getRetryCount(), event.getTopic(), event.getAggregateId(), e);
                }
            }
        }
    }
}
