package com.ott.core.modules.search.listener;

import com.ott.common.error.BusinessException;
import com.ott.common.error.ErrorCode;
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
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.stream.Stream;

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
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void handleVideoIndexRequest(VideoIndexRequestedEvent event) {
        log.debug("[Search] ES 검색 문서 동기화 시작: videoId={}", event.videoId());

            // 1. RDB에서 최신 메타데이터 조회
            VideoMetadata metadata = videoMetadataRepository.findByVideoIdAndDeleted(event.videoId(), false)
                    .orElseThrow(() -> {
                        log.warn("[Search] 동기화 대상 비디오를 찾을 수 없습니다. - videoId: {}", event.videoId());
                        return new BusinessException(ErrorCode.VIDEO_METADATA_NOT_FOUND);
                    });
        try {
            List<Tag> tags = videoTagRepository.findTagsWithParentByVideoMetadataId(metadata.getId());

            List<String> distinctTagNames = tags.stream()
                    .flatMap(tag -> tag.getParent() != null
                            ? Stream.of(tag.getTagName(), tag.getParent().getTagName())
                            : Stream.of(tag.getTagName()))
                    .distinct()
                    .toList();

            VideoDocument document = VideoDocument.from(metadata, distinctTagNames, metadata.getEmbedding());

            // 4. ES에 저장 (동일한 ID면 알아서 덮어쓰기 됨)
            videoSearchRepository.save(document);
            log.debug("[Search] ES 검색 문서 동기화 완료: videoId={}", event.videoId());

        } catch (Exception e) {
            log.warn("[Search] Elasticsearch 저장 중 오류 발생 (재시도 예정) - videoId: {}", event.videoId());
            throw new BusinessException(ErrorCode.ELASTICSEARCH_SYNC_ERROR);
        }
    }

    /**
     * 비디오 삭제 이벤트 처리
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void handleVideoIndexDelete(VideoIndexDeletedEvent event) {
        try {
            // ES의 문서를 완전히 삭제하여 검색에서 바로 내림
            videoSearchRepository.deleteById(event.videoId());
            log.debug("[Search] ES 검색 문서 삭제 완료: videoId={}", event.videoId());
        } catch (Exception e) {
            log.warn("[Search] Elasticsearch 삭제 중 오류 발생 (재시도 예정) - videoId: {}", event.videoId());
            throw new BusinessException(ErrorCode.ELASTICSEARCH_SYNC_ERROR);
        }
    }
    @Recover
    public void recover(Exception e, VideoIndexRequestedEvent event) {
        log.error("🚨 [Search] ES 검색 문서 동기화 최종 실패! 수동 복구(배치 동기화)가 필요합니다. - videoId: {}, 원인: {}", event.videoId(), e.getMessage());
    }

    @Recover
    public void recoverDelete(Exception e, VideoIndexDeletedEvent event) {
        log.error("🚨 [Search] ES 검색 문서 삭제 최종 실패! 엘라스틱서치에 좀비 데이터가 남아있을 수 있습니다. - videoId: {}, 원인: {}", event.videoId(), e.getMessage());
    }
}