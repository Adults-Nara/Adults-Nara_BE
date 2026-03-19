package com.ott.common.outbox.scheduler;

import com.ott.common.outbox.entity.OutboxEvent;
import com.ott.common.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * 아웃박스 이벤트 1건을 독립 트랜잭션 내에서 처리하는 컴포넌트.
 *
 * <p>OutboxPublisher(스케줄러)와 분리된 별도 빈으로 선언하여 Spring AOP 프록시를 통한
 * 트랜잭션이 올바르게 적용되도록 한다. 이벤트별 개별 트랜잭션을 사용함으로써
 * Kafka 장애 시 DB 커넥션이 100건 × 5초 = 최대 500초 동안 묶이는 문제를 방지한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventProcessor {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final int MAX_RETRY = 3;

    /**
     * 이벤트 1건을 독립 트랜잭션에서 Kafka로 발행하고 상태를 갱신한다.
     *
     * <p>트랜잭션 범위: DB 조회 → Kafka 동기 전송(5초 timeout) → DB 상태 업데이트.
     * Kafka 전송이 실패해도 해당 이벤트의 트랜잭션만 롤백되며 나머지 이벤트에 영향을 주지 않는다.</p>
     *
     * @param eventId 처리할 OutboxEvent의 ID
     */
    @Transactional
    public void process(Long eventId) {
        OutboxEvent event = outboxEventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalStateException("OutboxEvent를 찾을 수 없음: id=" + eventId));

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
