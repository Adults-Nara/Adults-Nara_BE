package com.ott.core.modules.search;
import com.ott.common.persistence.entity.Tag;
import com.ott.common.persistence.entity.VideoMetadata;
import com.ott.common.persistence.enums.VideoType;
import com.ott.core.modules.search.document.VideoDocument;
import com.ott.core.modules.search.event.VideoIndexDeletedEvent;
import com.ott.core.modules.search.event.VideoIndexRequestedEvent;
import com.ott.core.modules.search.listener.VideoSearchEventListener;
import com.ott.core.modules.search.repository.VideoSearchRepository;
import com.ott.core.modules.tag.repository.VideoTagRepository;
import com.ott.core.modules.video.repository.VideoMetadataRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VideoSearchEventListenerTest {
    @InjectMocks
    private VideoSearchEventListener videoSearchEventListener;

    @Mock
    private VideoMetadataRepository videoMetadataRepository;

    @Mock
    private VideoTagRepository videoTagRepository;

    @Mock
    private VideoSearchRepository videoSearchRepository;

    @Test
    @DisplayName("트랜스코딩 완료 이벤트 수신 시, 메타데이터와 태그를 조합하여 ES에 정상적으로 저장해야 한다.")
    void handleVideoIndexRequest_Success() {
        // given (상황 세팅)
        Long videoId = 100L;
        Long metadataId = 1L;

        // 1. 가짜 VideoMetadata 엔티티 생성
        VideoMetadata mockMetadata = VideoMetadata.builder()
                .videoId(videoId)
                .title("아이언맨 테스트 영상")
                .description("테스트 설명입니다.")
                .videoType(VideoType.LONG)
                .viewCount(10)
                .likeCount(5)
                .build();
        ReflectionTestUtils.setField(mockMetadata, "id", metadataId); // JPA ID 강제 주입

        // 2. 가짜 태그 데이터 생성 (부모 태그가 있는 경우와 없는 경우 혼합)
        Tag parentTag = createMockTag(10L, "영화", null);
        Tag childTag1 = createMockTag(11L, "액션", parentTag);
        Tag childTag2 = createMockTag(12L, "마블", null); // 부모 없는 독립 태그

        List<Tag> mockTags = List.of(childTag1, childTag2);

        // 3. Mock 객체 행동 정의 (Stubbing)
        when(videoMetadataRepository.findByVideoIdAndDeleted(videoId, false))
                .thenReturn(Optional.of(mockMetadata));
        when(videoTagRepository.findTagsWithParentByVideoMetadataId(metadataId))
                .thenReturn(mockTags);

        // 이벤트 객체 생성
        VideoIndexRequestedEvent event = new VideoIndexRequestedEvent(videoId);

        // when (테스트 대상 실행)
        videoSearchEventListener.handleVideoIndexRequest(event);

        // then (결과 검증)
        // 1. ES repository의 save 메서드가 정확히 1번 호출되었는지 검증 및 저장된 객체 캡처
        ArgumentCaptor<VideoDocument> documentCaptor = ArgumentCaptor.forClass(VideoDocument.class);
        verify(videoSearchRepository, times(1)).save(documentCaptor.capture());

        VideoDocument savedDocument = documentCaptor.getValue();

        // 2. 저장된 Document의 데이터가 올바르게 매핑되었는지 꼼꼼하게 확인
        assertThat(savedDocument.getVideoId()).isEqualTo(videoId);
        assertThat(savedDocument.getTitle()).isEqualTo("아이언맨 테스트 영상");
        assertThat(savedDocument.getDescription()).isEqualTo("테스트 설명입니다.");
        assertThat(savedDocument.getVideoType()).isEqualTo(VideoType.LONG);

        // 3. 부모 태그("영화")와 자식 태그("액션", "마블")가 중복 없이 평탄화(flatMap) 되었는지 검증
        assertThat(savedDocument.getTags()).containsExactlyInAnyOrder("영화", "액션", "마블");
    }

    @Test
    @DisplayName("DB에 해당 videoId의 메타데이터가 없으면, ES 저장을 수행하지 않고 예외 로그를 남기며 종료된다.")
    void handleVideoIndexRequest_Fail_NotFound() {
        // given
        Long invalidVideoId = 999L;
        when(videoMetadataRepository.findByVideoIdAndDeleted(invalidVideoId, false))
                .thenReturn(Optional.empty()); // DB에 데이터 없음

        VideoIndexRequestedEvent event = new VideoIndexRequestedEvent(invalidVideoId);

        // when
        videoSearchEventListener.handleVideoIndexRequest(event);

        // then
        // ES 저장이 절대 호출되지 않아야 함 (예외를 catch해서 먹어버리므로 throw 되진 않음)
        verify(videoSearchRepository, never()).save(any(VideoDocument.class));
    }

    @Test
    @DisplayName("비디오 삭제 이벤트 수신 시, ES에서 해당 문서를 정상적으로 삭제해야 한다.")
    void handleVideoIndexDelete_Success() {
        // given
        Long videoIdToDelete = 200L;
        VideoIndexDeletedEvent event = new VideoIndexDeletedEvent(videoIdToDelete);

        // when
        videoSearchEventListener.handleVideoIndexDelete(event);

        // then
        verify(videoSearchRepository, times(1)).deleteById(videoIdToDelete);
    }

    // 테스트용 태그 생성 유틸리티 메서드
    private Tag createMockTag(Long id, String name, Tag parent) {
        // 1. public 생성자 사용
        Tag tag = new Tag(name);

        // 2. public setter 사용
        tag.setParent(parent);

        // 3. ID는 setter가 없으므로 ReflectionTestUtils로 주입
        ReflectionTestUtils.setField(tag, "id", id);

        return tag;
    }
}
