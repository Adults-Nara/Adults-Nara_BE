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

import java.util.*;
import java.util.function.Function;
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
            List<VideoTag> videoTags = videoTagRepository.findWithTagAndParentByVideoMetadataIdIn(List.of(metadata.getId()));

            Map<TagSource, Set<String>> tagsBySource = videoTags.stream()
                    .collect(Collectors.groupingBy(VideoTag::getSource,
                            Collectors.flatMapping(vt -> vt.getTag().getParent() != null
                                            ? Stream.of(vt.getTag().getTagName(), vt.getTag().getParent().getTagName())
                                            : Stream.of(vt.getTag().getTagName()),
                                    Collectors.toSet())));

            Set<String> userTags = tagsBySource.getOrDefault(TagSource.USER, Set.of());
            Set<String> aiTags = tagsBySource.getOrDefault(TagSource.AI, Set.of());

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
    // 엘라스틱서치에서 문서 즉시 삭제
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void deleteFromElasticsearch(List<Long> videoIds) {
        try {
            videoSearchRepository.deleteAllById(videoIds);
            log.info("[Search] ES 검색 문서 삭제 완료 (백오피스 삭제 연동): videoId={}", videoIds);
        } catch (Exception e) {
            log.error("[Search] ES 검색 문서 삭제 실패: videoId={}, 원인: {}", videoIds, e.getMessage());
            throw new BusinessException(ErrorCode.ELASTICSEARCH_DELETE_ERROR, e);
        }
    }

    // 최종 실패 시 호출됨 (Event 객체 대신 videoId를 직접 받음)
    @Recover
    public void recover(Exception e, Long videoId) {
        log.error("🚨 [Search] ES 검색 문서 동기화 최종 실패! 수동 복구(배치 동기화)가 필요합니다. - videoId: {}, 원인: {}", videoId, e.getMessage());
    }
    @Recover
    public void recoverBulk(Exception e, List<Long> videoIds) {
        log.error("🚨 [Search] ES 벌크 작업(동기화/삭제) 최종 실패! 수동 복구가 필요합니다. - videoIds: {}, 원인: {}", videoIds, e.getMessage());
    }

    // =========================================================================
    // 벌크 동기화 메서드 (N+1 성능 문제 해결)
    // =========================================================================
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void bulkSyncToElasticsearch(List<Long> videoIds) {
        if (videoIds == null || videoIds.isEmpty()) return;

        log.info("[Search] ES 검색 문서 벌크 동기화 시작: 대상 {}건", videoIds.size());
        try {
            // 1. 메타데이터 일괄 조회
            List<VideoMetadata> metadataList = videoMetadataRepository.findAllByVideoIdIn(videoIds);
            if (metadataList.isEmpty()) return;

            List<Long> metadataIds = metadataList.stream().map(VideoMetadata::getId).toList();

            // 2. 태그 및 AI 데이터 일괄 조회 (N+1 방어)
            List<VideoTag> allTags = videoTagRepository.findWithTagAndParentByVideoMetadataIdIn(metadataIds);
            Map<Long, List<VideoTag>> tagsByMetadataId = allTags.stream()
                    .collect(Collectors.groupingBy(vt -> vt.getVideoMetadata().getId()));

            List<VideoAiAnalysis> aiAnalysisList = videoAiAnalysisRepository.findAllById(videoIds);
            Map<Long, VideoAiAnalysis> aiAnalysisMap = aiAnalysisList.stream()
                    .collect(Collectors.toMap(VideoAiAnalysis::getId, Function.identity()));

            // 3. Document 일괄 조립
            List<VideoDocument> documents = metadataList.stream().map(metadata -> {
                List<VideoTag> vTags = tagsByMetadataId.getOrDefault(metadata.getId(), List.of());

                Map<TagSource, Set<String>> tagsBySource = vTags.stream()
                        .collect(Collectors.groupingBy(VideoTag::getSource,
                                Collectors.flatMapping(vt -> vt.getTag().getParent() != null
                                                ? Stream.of(vt.getTag().getTagName(), vt.getTag().getParent().getTagName())
                                                : Stream.of(vt.getTag().getTagName()),
                                        Collectors.toSet())));

                Set<String> userTags = tagsBySource.getOrDefault(TagSource.USER, Set.of());
                Set<String> aiTags = tagsBySource.getOrDefault(TagSource.AI, Set.of());

                List<String> matchedTags = new ArrayList<>(userTags);
                matchedTags.retainAll(aiTags);

                Set<String> allTagsSet = new HashSet<>(userTags);
                allTagsSet.addAll(aiTags);
                List<String> distinctTagNames = new ArrayList<>(allTagsSet);

                VideoAiAnalysis aiAnalysis = aiAnalysisMap.get(metadata.getVideoId());
                return VideoDocument.from(metadata, distinctTagNames, matchedTags, aiAnalysis);
            }).toList();

            // 4. ES 벌크 저장 (한 번의 통신으로 끝!)
            videoSearchRepository.saveAll(documents);
            log.info("[Search] ES 검색 문서 벌크 동기화 완료: {}건", documents.size());

        } catch (Exception e) {
            log.error("[Search] ES 벌크 동기화 실패", e);
            throw new BusinessException(ErrorCode.ELASTICSEARCH_SYNC_ERROR, e);
        }
    }
}