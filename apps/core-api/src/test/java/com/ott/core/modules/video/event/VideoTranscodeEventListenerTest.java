package com.ott.core.modules.video.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ott.common.outbox.entity.OutboxEvent;
import com.ott.common.outbox.enums.OutboxStatus;
import com.ott.common.outbox.repository.OutboxEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VideoTranscodeEventListenerTest {

    @InjectMocks
    private VideoTranscodeEventListener listener;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("트랜스코딩 요청 이벤트 수신 시 OutboxEvent가 저장된다")
    void handleTranscodeRequest_savesOutboxEvent() {
        // given
        Long videoId = 12345L;
        VideoTranscodeRequestedEvent event = new VideoTranscodeRequestedEvent(videoId);

        given(outboxEventRepository.save(any(OutboxEvent.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        listener.handleTranscodeRequest(event);

        // then
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());

        OutboxEvent saved = captor.getValue();
        assertThat(saved.getAggregateType()).isEqualTo("Video");
        assertThat(saved.getAggregateId()).isEqualTo("12345");
        assertThat(saved.getEventType()).isEqualTo("VideoTranscodeRequested");
        assertThat(saved.getTopic()).isEqualTo("video-transcode-requested");
        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(saved.getRetryCount()).isZero();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("저장된 OutboxEvent의 payload에 videoId가 포함된다")
    void handleTranscodeRequest_payloadContainsVideoId() throws JsonProcessingException {
        // given
        Long videoId = 99999L;
        VideoTranscodeRequestedEvent event = new VideoTranscodeRequestedEvent(videoId);

        given(outboxEventRepository.save(any(OutboxEvent.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        listener.handleTranscodeRequest(event);

        // then
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());

        String payload = captor.getValue().getPayload();
        assertThat(payload).contains("99999");

        // payload가 유효한 JSON인지 확인
        var parsed = objectMapper.readTree(payload);
        assertThat(parsed.get("videoId").asLong()).isEqualTo(99999L);
    }

    @Test
    @DisplayName("ObjectMapper 직렬화 실패 시 RuntimeException이 발생한다")
    void handleTranscodeRequest_serializationFailure() throws JsonProcessingException {
        // given
        Long videoId = 1L;
        VideoTranscodeRequestedEvent event = new VideoTranscodeRequestedEvent(videoId);

        given(objectMapper.writeValueAsString(event))
                .willThrow(new JsonProcessingException("Serialization error") {});

        // when & then
        assertThatThrownBy(() -> listener.handleTranscodeRequest(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Outbox 이벤트 직렬화 실패");
    }
}
