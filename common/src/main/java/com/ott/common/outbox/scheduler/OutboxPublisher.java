package com.ott.common.outbox.scheduler;

import com.ott.common.outbox.enums.OutboxStatus;
import com.ott.common.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventProcessor outboxEventProcessor;

    /**
     * PENDING 상태의 이벤트 ID 목록을 조회한 뒤,
     * 이벤트별로 OutboxEventProcessor에 처리를 위임한다.
     *
     * <p>
     * 이 메서드 자체는 @Transactional을 갖지 않으며, 트랜잭션은 이벤트 1건 단위로
     * OutboxEventProcessor.process() 안에서만 열린다. Kafka 장애 시에도
     * DB 커넥션이 전체 배치 동안 묶이는 문제가 발생하지 않는다.
     * </p>
     */
    @Scheduled(fixedDelay = 1000)
    public void publishPendingEvents() {
        List<Long> pendingIds = outboxEventRepository
                .findIdsByStatus(OutboxStatus.PENDING, PageRequest.of(0, 100));

        for (Long id : pendingIds) {
            try {
                outboxEventProcessor.process(id);
            } catch (Exception e) {
                log.error("[outbox] 이벤트 처리 중 예외 발생, 다음 이벤트로 계속 진행: id={}", id, e);
            }
        }
    }
}
