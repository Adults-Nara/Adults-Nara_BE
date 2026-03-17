package com.ott.core.modules.search.service;

import com.ott.common.error.BusinessException;
import com.ott.common.error.ErrorCode;
import com.ott.common.persistence.entity.VideoAiAnalysis;
import com.ott.common.persistence.entity.VideoMetadata;
import com.ott.common.persistence.entity.VideoTag;
import com.ott.common.persistence.enums.TagSource;
import com.ott.core.modules.ai.repository.VideoAiAnalysisRepository;
import com.ott.core.modules.search.document.VideoDocument;
import com.ott.core.modules.search.repository.VideoSearchRepository;
import com.ott.core.modules.tag.repository.VideoTagRepository;
import com.ott.core.modules.video.repository.VideoMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoSearchSyncService {

    private final VideoMetadataRepository videoMetadataRepository;
    private final VideoTagRepository videoTagRepository;
    private final VideoSearchRepository videoSearchRepository;
    private final VideoAiAnalysisRepository videoAiAnalysisRepository;

    // 단일 비디오 동기화 로직
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void syncToElasticsearch(Long videoId) {
        log.info("[Search] ES 검색 문서 동기화 시작: videoId={}", videoId);
        try {
            Optional<VideoMetadata> metadataOpt = videoMetadataRepository.findByVideoId(videoId);

            if (metadataOpt.isEmpty()) {
                log.warn("[Search] DB에서 비디오를 찾을 수 없어 ES 문서를 삭제합니다. - videoId: {}", videoId);
                videoSearchRepository.deleteById(videoId);
                return;
            }

            VideoMetadata metadata = metadataOpt.get();

            // Tag 대신 VideoTag를 가져와야 Source(USER/AI) 구분이 가능
            // (Bulk Sync에서 쓰시던 N+1 방지용 메서드를 재활용합니다)
            List<VideoTag> videoTags = videoTagRepository.findWithTagAndParentByVideoMetadataIdIn(List.of(metadata.getId()));

            // USER가 등록한 태그 추출 (부모 태그 포함)
            Set<String> userTags = videoTags.stream()
                    .filter(vt -> vt.getSource() == TagSource.USER)
                    .flatMap(vt -> vt.getTag().getParent() != null
                            ? Stream.of(vt.getTag().getTagName(), vt.getTag().getParent().getTagName())
                            : Stream.of(vt.getTag().getTagName()))
                    .collect(Collectors.toSet());

            // AI가 추출한 태그 추출
            Set<String> aiTags = videoTags.stream()
                    .filter(vt -> vt.getSource() == TagSource.AI)
                    .flatMap(vt -> vt.getTag().getParent() != null
                            ? Stream.of(vt.getTag().getTagName(), vt.getTag().getParent().getTagName())
                            : Stream.of(vt.getTag().getTagName()))
                    .collect(Collectors.toSet());

            // 교집합 계산
            List<String> matchedTags = new ArrayList<>(userTags);
            matchedTags.retainAll(aiTags);

            // 합집합 계산
            Set<String> allTagsSet = new HashSet<>(userTags);
            allTagsSet.addAll(aiTags);
            List<String> distinctTagNames = new ArrayList<>(allTagsSet);

            VideoAiAnalysis aiAnalysis = videoAiAnalysisRepository.findById(metadata.getVideoId())
                    .orElse(null);

            // 계산된 4개의 파라미터를 정확하게 전달
            VideoDocument document = VideoDocument.from(metadata, distinctTagNames, matchedTags, aiAnalysis);

            videoSearchRepository.save(document);
            log.info("[Search] ES 검색 문서 동기화 완료: videoId={}", videoId);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("ES 동기화 알 수 없는 에러", e);
            throw new BusinessException(ErrorCode.ELASTICSEARCH_SYNC_ERROR, e);
        }
    }

    // 최종 실패 시 호출됨 (Event 객체 대신 videoId를 직접 받음)
    @Recover
    public void recover(Exception e, Long videoId) {
        log.error("🚨 [Search] ES 검색 문서 동기화 최종 실패! 수동 복구(배치 동기화)가 필요합니다. - videoId: {}, 원인: {}", videoId, e.getMessage());
    }
}