package com.ott.core.outbox.scheduler;

import com.ott.common.outbox.entity.OutboxEvent;
import com.ott.common.outbox.enums.OutboxStatus;
import com.ott.common.outbox.repository.OutboxEventRepository;
import com.ott.common.outbox.scheduler.OutboxPublisher;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
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
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    @DisplayName("PENDING 이벤트가 없으면 아무 작업도 하지 않는다")
    void publishPendingEvents_noEvents() {
        // given
        given(outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .willReturn(Collections.emptyList());

        // when
        outboxPublisher.publishPendingEvents();

        // then
        verify(kafkaTemplate, never()).send(any(String.class), any(String.class), any());
    }

    @Test
    @DisplayName("PENDING 이벤트 발행 성공 시 PUBLISHED 상태로 전환된다")
    void publishPendingEvents_success() {
        // given
        OutboxEvent event = OutboxEvent.create(
                "Video", "123", "VideoTranscodeRequested",
                "video-transcode-requested", "{\"videoId\":123}"
        );

        given(outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .willReturn(List.of(event));

        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition("video-transcode-requested", 0), 0L, 0, 0L, 0, 0);
        SendResult<String, Object> sendResult = new SendResult<>(
                new ProducerRecord<>("video-transcode-requested", "123", "{\"videoId\":123}"),
                metadata
        );
        future.complete(sendResult);

        given(kafkaTemplate.send("video-transcode-requested", "123", "{\"videoId\":123}"))
                .willReturn(future);

        // when
        outboxPublisher.publishPendingEvents();

        // then
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(event.getPublishedAt()).isNotNull();
    }

    @Test
    @DisplayName("Kafka 발행 실패 시 retryCount가 증가한다")
    void publishPendingEvents_failureIncrementsRetry() {
        // given
        OutboxEvent event = OutboxEvent.create(
                "Video", "456", "VideoTranscodeRequested",
                "video-transcode-requested", "{\"videoId\":456}"
        );

        given(outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .willReturn(List.of(event));

        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka broker unavailable"));

        given(kafkaTemplate.send("video-transcode-requested", "456", "{\"videoId\":456}"))
                .willReturn(future);

        // when
        outboxPublisher.publishPendingEvents();

        // then
        assertThat(event.getRetryCount()).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
    }

    @Test
    @DisplayName("최대 재시도 횟수 초과 시 FAILED 상태로 전환된다")
    void publishPendingEvents_maxRetryExceeded_marksFailed() {
        // given
        OutboxEvent event = OutboxEvent.create(
                "Video", "789", "VideoTranscodeRequested",
                "video-transcode-requested", "{\"videoId\":789}"
        );
        // 이미 2회 실패한 상태 시뮬레이션
        event.incrementRetry();
        event.incrementRetry();

        given(outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .willReturn(List.of(event));

        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka broker unavailable"));

        given(kafkaTemplate.send("video-transcode-requested", "789", "{\"videoId\":789}"))
                .willReturn(future);

        // when
        outboxPublisher.publishPendingEvents();

        // then
        assertThat(event.getRetryCount()).isEqualTo(3);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
    }

    @Test
    @DisplayName("여러 이벤트 중 일부만 실패해도 나머지는 정상 발행된다")
    void publishPendingEvents_partialFailure() {
        // given
        OutboxEvent successEvent = OutboxEvent.create(
                "Video", "100", "VideoTranscodeRequested",
                "video-transcode-requested", "{\"videoId\":100}"
        );
        OutboxEvent failEvent = OutboxEvent.create(
                "Video", "200", "VideoTranscodeRequested",
                "video-transcode-requested", "{\"videoId\":200}"
        );

        given(outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .willReturn(List.of(successEvent, failEvent));

        // 성공 이벤트
        CompletableFuture<SendResult<String, Object>> successFuture = new CompletableFuture<>();
        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition("video-transcode-requested", 0), 0L, 0, 0L, 0, 0);
        successFuture.complete(new SendResult<>(
                new ProducerRecord<>("video-transcode-requested", "100", "{\"videoId\":100}"),
                metadata
        ));
        given(kafkaTemplate.send("video-transcode-requested", "100", "{\"videoId\":100}"))
                .willReturn(successFuture);

        // 실패 이벤트
        CompletableFuture<SendResult<String, Object>> failFuture = new CompletableFuture<>();
        failFuture.completeExceptionally(new RuntimeException("Kafka error"));
        given(kafkaTemplate.send("video-transcode-requested", "200", "{\"videoId\":200}"))
                .willReturn(failFuture);

        // when
        outboxPublisher.publishPendingEvents();

        // then
        assertThat(successEvent.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(failEvent.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(failEvent.getRetryCount()).isEqualTo(1);
    }
}
