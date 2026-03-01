package com.ott.core.modules.search.listener;

import com.ott.common.persistence.entity.Tag;
import com.ott.common.persistence.entity.VideoMetadata;
import com.ott.core.modules.search.document.VideoDocument;
import com.ott.core.modules.search.event.VideoIndexDeletedEvent;
import com.ott.core.modules.search.event.VideoIndexRequestedEvent;
import com.ott.core.modules.search.repository.VideoSearchRepository;
import com.ott.core.modules.tag.repository.VideoTagRepository;
import com.ott.core.modules.video.repository.VideoMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoSearchEventListener {

    private final VideoMetadataRepository videoMetadataRepository;
    private final VideoTagRepository videoTagRepository;
    private final VideoSearchRepository videoSearchRepository;

    /**
     * DB 저장이 완료된 후, ES에 문서를 색인
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void handleVideoIndexRequest(VideoIndexRequestedEvent event) {
        log.info("ES 검색 문서 동기화 시작: videoId={}", event.videoId());

        try {
            // 1. RDB에서 최신 메타데이터 조회
            VideoMetadata metadata = videoMetadataRepository.findByVideoIdAndDeleted(event.videoId(), false)
                    .orElseThrow(() -> new IllegalArgumentException("VideoMetadata not found for videoId: " + event.videoId()));

            // 2. 관련 태그 이름들만 추출 (List<String>)
            List<Tag> tags = videoTagRepository.findTagsByVideoMetadataId(metadata.getId());
            List<String> tagNames = new ArrayList<>();

            for (Tag tag : tags) {
                tagNames.add(tag.getTagName());

                // 상위 태그가 있다면 ES에 같이 넣어줍니다.
                if (tag.getParent() != null) {
                    tagNames.add(tag.getParent().getTagName());
                }
            }

            List<String> distinctTagNames = tagNames.stream().distinct().toList();

            // 3. Document 객체 변환
            VideoDocument document = VideoDocument.builder()
                    .videoId(metadata.getVideoId())
                    .metadataId(metadata.getId()) // RDB 매핑용
                    .userId(metadata.getUserId())
                    .title(metadata.getTitle())
                    .description(metadata.getDescription())
                    .videoType(metadata.getVideoType())
                    .tags(distinctTagNames) // 계층형 태그 이름들이 모두 담김
                    .viewCount(metadata.getViewCount())
                    .likeCount(metadata.getLikeCount())
                    .deleted(metadata.isDeleted())
                    .thumbnailUrl(metadata.getThumbnailUrl())
                    .duration(metadata.getDuration())
                    .createdAt(metadata.getCreatedAt())
                    .build();

            // 4. ES에 저장 (동일한 ID면 알아서 덮어쓰기 됨)
            videoSearchRepository.save(document);
            log.info("ES 검색 문서 동기화 완료: videoId={}", event.videoId());

        } catch (Exception e) {
            // 비동기 큐가 없으므로 여기서 에러 로깅 후 알림 처리를 하는 것이 좋습니다. (예: Slack 알림)
            log.error("ES 검색 문서 동기화 실패: videoId={}", event.videoId(), e);
        }
    }

    /**
     * 비디오 삭제 이벤트 처리 (ES에서도 완전히 지우거나, 삭제 상태로 덮어씀)
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleVideoIndexDelete(VideoIndexDeletedEvent event) {
        log.info("ES 검색 문서 삭제 시작: videoId={}", event.videoId());
        try {
            // ES의 문서를 완전히 삭제하여 검색에서 바로 내림 (Soft Delete를 유지하려면 save()로 업데이트)
            videoSearchRepository.deleteById(event.videoId());
            log.info("ES 검색 문서 삭제 완료: videoId={}", event.videoId());
        } catch (Exception e) {
            log.error("ES 검색 문서 삭제 실패: videoId={}", event.videoId(), e);
        }
    }
}