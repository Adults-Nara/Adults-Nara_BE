package com.ott.core.modules.ai.service;

import com.ott.common.error.BusinessException;
import com.ott.common.error.ErrorCode;
import com.ott.common.persistence.entity.Tag;
import com.ott.common.persistence.entity.VideoAiAnalysis;
import com.ott.common.persistence.entity.VideoMetadata;
import com.ott.common.persistence.entity.VideoTag;
import com.ott.core.modules.ai.dto.VideoAiAnalysisCompletedEvent;
import com.ott.core.modules.ai.repository.VideoAiAnalysisRepository;
import com.ott.core.modules.search.event.VideoIndexRequestedEvent;
import com.ott.core.modules.tag.repository.TagRepository;
import com.ott.core.modules.tag.repository.VideoTagRepository;
import com.ott.core.modules.video.repository.VideoMetadataRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoAiAnalysisServiceTest {

    @InjectMocks
    private VideoAiAnalysisService videoAiAnalysisService;

    @Mock
    private VideoMetadataRepository videoMetadataRepository;
    @Mock
    private VideoAiAnalysisRepository videoAiAnalysisRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private VideoTagRepository videoTagRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("AI 분석 결과 처리 성공 - 새로운 태그와 분석 결과 저장")
    void processAnalysisResult_Success() {
        // given
        Long videoId = 100L;
        VideoAiAnalysisCompletedEvent event = new VideoAiAnalysisCompletedEvent(
                videoId, "COMPLETED", List.of("액션", "SF"), "요약문", "subtitle.vtt", new float[] { 0.1f, 0.2f }, null);

        VideoMetadata metadata = mock(VideoMetadata.class);
        given(metadata.getVideoId()).willReturn(videoId);

        Tag tag1 = mock(Tag.class);
        Tag tag2 = mock(Tag.class);

        given(videoMetadataRepository.findByVideoId(videoId)).willReturn(Optional.of(metadata));
        given(tagRepository.findByTagNameIn(event.aiTags())).willReturn(List.of(tag1, tag2));
        given(videoTagRepository.findAllByVideoMetadataIdAndSource(any(), eq(com.ott.common.persistence.enums.TagSource.AI))).willReturn(List.of());
        given(videoAiAnalysisRepository.findById(videoId)).willReturn(Optional.empty());

        // when
        videoAiAnalysisService.processAnalysisResult(event);

        // then
        // 1. 비디오 태그 저장 여부 확인 (2개)
        verify(videoTagRepository, times(2)).save(any(VideoTag.class));

        // 2. VideoAiAnalysis 엔티티 저장 여부 확인
        verify(videoAiAnalysisRepository).save(any(VideoAiAnalysis.class));

        // 3. 이벤트 발행 여부 확인
        verify(eventPublisher).publishEvent(any(VideoIndexRequestedEvent.class));
    }

    @Test
    @DisplayName("비디오 메타데이터를 찾을 수 없는 경우 예외 발생")
    void processAnalysisResult_ThrowsException_WhenMetadataNotFound() {
        // given
        Long videoId = 100L;
        VideoAiAnalysisCompletedEvent event = new VideoAiAnalysisCompletedEvent(
                videoId, "COMPLETED", List.of("액션"), "요약문", null, new float[] { 0.1f }, null);

        given(videoMetadataRepository.findByVideoId(videoId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> videoAiAnalysisService.processAnalysisResult(event))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.VIDEO_METADATA_NOT_FOUND.getMessage());

        verifyNoInteractions(tagRepository, videoTagRepository, videoAiAnalysisRepository, eventPublisher);
    }

    @Test
    @DisplayName("DB에 존재하지 않는 AI 태그가 반환될 경우 해당 태그는 무시하고 진행한다")
    void processAnalysisResult_Ignore_WhenTagNotFound() {
        // given
        Long videoId = 100L;
        VideoAiAnalysisCompletedEvent event = new VideoAiAnalysisCompletedEvent(
                videoId, "COMPLETED", List.of("없는태그"), "요약문", null, new float[] { 0.1f }, null);

        VideoMetadata metadata = mock(VideoMetadata.class);
        given(metadata.getVideoId()).willReturn(videoId);

        given(videoMetadataRepository.findByVideoId(videoId)).willReturn(Optional.of(metadata));
        given(tagRepository.findByTagNameIn(event.aiTags())).willReturn(List.of()); // DB에 태그 없음
        given(videoTagRepository.findAllByVideoMetadataIdAndSource(any(), any())).willReturn(List.of());
        given(videoAiAnalysisRepository.findById(videoId)).willReturn(Optional.empty());

        // when
        videoAiAnalysisService.processAnalysisResult(event);

        // then
        // 태그 저장은 호출되지 않음
        verify(videoTagRepository, never()).save(any(VideoTag.class));
        
        // 나머지 과정은 정상 진행
        verify(videoAiAnalysisRepository).save(any(VideoAiAnalysis.class));
        verify(eventPublisher).publishEvent(any(VideoIndexRequestedEvent.class));
    }

    @Test
    @DisplayName("이미 분석 결과가 존재할 경우 새 분석 결과를 덮어쓰지 않고 태그와 이벤트만 처리")
    void processAnalysisResult_SkipAnalysisSave_WhenAlreadyExists() {
        // given
        Long videoId = 100L;
        VideoAiAnalysisCompletedEvent event = new VideoAiAnalysisCompletedEvent(
                videoId, "COMPLETED", List.of("액션"), "새로운 요약문", null, new float[] { 0.1f }, null);

        VideoMetadata metadata = mock(VideoMetadata.class);
        given(metadata.getVideoId()).willReturn(videoId);

        Tag tag1 = mock(Tag.class);
        VideoAiAnalysis existingAnalysis = mock(VideoAiAnalysis.class);

        given(videoMetadataRepository.findByVideoId(videoId)).willReturn(Optional.of(metadata));
        given(tagRepository.findByTagNameIn(event.aiTags())).willReturn(List.of(tag1));
        given(videoTagRepository.findAllByVideoMetadataIdAndSource(any(), eq(com.ott.common.persistence.enums.TagSource.AI))).willReturn(List.of());
        // 이미 분석 결과가 존재하는 상황
        given(videoAiAnalysisRepository.findById(videoId)).willReturn(Optional.of(existingAnalysis));

        // when
        videoAiAnalysisService.processAnalysisResult(event);

        // then
        // 태그는 1건 저장됨
        verify(videoTagRepository, times(1)).save(any(VideoTag.class));

        // 분석 결과 저장은 호출되지 않음 (Skip)
        verify(videoAiAnalysisRepository, never()).save(any(VideoAiAnalysis.class));

        // 이벤트는 발행됨
        verify(eventPublisher).publishEvent(any(VideoIndexRequestedEvent.class));
    }

    @Test
    @DisplayName("이미 동일한 AI 태그가 존재할 경우 저장을 스킵한다 (멱등성)")
    void processAnalysisResult_SkipTagSave_WhenTagAlreadyExists() {
        // given
        Long videoId = 100L;
        VideoAiAnalysisCompletedEvent event = new VideoAiAnalysisCompletedEvent(
                videoId, "COMPLETED", List.of("액션"), "요약", null, new float[] { 0.1f }, null);

        VideoMetadata metadata = mock(VideoMetadata.class);
        given(metadata.getId()).willReturn(1L);
        given(metadata.getVideoId()).willReturn(videoId);

        Tag tag1 = mock(Tag.class);
        given(tag1.getId()).willReturn(500L);
        given(tag1.getTagName()).willReturn("액션");

        // 이미 동일한 태그가 AI 소스로 저장되어 있는 상황
        VideoTag existingVideoTag = mock(VideoTag.class);
        given(existingVideoTag.getTag()).willReturn(tag1);

        given(videoMetadataRepository.findByVideoId(videoId)).willReturn(Optional.of(metadata));
        given(tagRepository.findByTagNameIn(event.aiTags())).willReturn(List.of(tag1));
        given(videoTagRepository.findAllByVideoMetadataIdAndSource(metadata.getId(), com.ott.common.persistence.enums.TagSource.AI))
                .willReturn(List.of(existingVideoTag));
        given(videoAiAnalysisRepository.findById(videoId)).willReturn(Optional.empty());

        // when
        videoAiAnalysisService.processAnalysisResult(event);

        // then
        // 태그 저장은 호출되지 않음 (Skip)
        verify(videoTagRepository, never()).save(any(VideoTag.class));
        
        // 나머지 과정은 정상 진행
        verify(videoAiAnalysisRepository).save(any(VideoAiAnalysis.class));
        verify(eventPublisher).publishEvent(any(VideoIndexRequestedEvent.class));
    }
}
