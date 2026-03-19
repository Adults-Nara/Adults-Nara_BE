package com.ott.core.outbox.scheduler;

import com.ott.common.outbox.enums.OutboxStatus;
import com.ott.common.outbox.repository.OutboxEventRepository;
import com.ott.common.outbox.scheduler.OutboxEventProcessor;
import com.ott.common.outbox.scheduler.OutboxPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @InjectMocks
    private OutboxPublisher outboxPublisher;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private OutboxEventProcessor outboxEventProcessor;

    @Test
    @DisplayName("PENDING 이벤트가 없으면 processor를 호출하지 않는다")
    void publishPendingEvents_noEvents() {
        // given
        given(outboxEventRepository.findIdsByStatus(eq(OutboxStatus.PENDING), any(Pageable.class)))
                .willReturn(Collections.emptyList());

        // when
        outboxPublisher.publishPendingEvents();

        // then
        verify(outboxEventProcessor, never()).process(any());
    }

    @Test
    @DisplayName("PENDING 이벤트가 있으면 각 ID에 대해 processor.process()를 호출한다")
    void publishPendingEvents_callsProcessorForEachId() {
        // given
        List<Long> ids = List.of(1L, 2L, 3L);
        given(outboxEventRepository.findIdsByStatus(eq(OutboxStatus.PENDING), any(Pageable.class)))
                .willReturn(ids);

        // when
        outboxPublisher.publishPendingEvents();

        // then
        verify(outboxEventProcessor, times(1)).process(1L);
        verify(outboxEventProcessor, times(1)).process(2L);
        verify(outboxEventProcessor, times(1)).process(3L);
    }

    @Test
    @DisplayName("processor.process() 중 하나가 예외를 던져도 다른 이벤트는 계속 처리된다")
    void publishPendingEvents_processorExceptionDoesNotStopOtherEvents() {
        // given
        List<Long> ids = List.of(10L, 20L, 30L);
        given(outboxEventRepository.findIdsByStatus(eq(OutboxStatus.PENDING), any(Pageable.class)))
                .willReturn(ids);

        // 20L 처리 시에만 예외 발생
        doThrow(new RuntimeException("Kafka unavailable"))
                .when(outboxEventProcessor).process(20L);

        // when — publisher 내부에서 예외를 catch하므로 publishPendingEvents 자체는 완료되어야 함
        outboxPublisher.publishPendingEvents();

        // then: 예외가 발생한 20L 전후로 10L, 30L도 모두 호출되었는지 확인
        verify(outboxEventProcessor).process(10L);
        verify(outboxEventProcessor).process(20L);
        verify(outboxEventProcessor).process(30L);
    }
}
