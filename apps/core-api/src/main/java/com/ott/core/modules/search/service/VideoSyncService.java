package com.ott.core.modules.search.service;

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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoSyncService {

    private final VideoMetadataRepository videoMetadataRepository;
    private final VideoTagRepository videoTagRepository;
    private final VideoSearchRepository videoSearchRepository;
    private final VideoAiAnalysisRepository videoAiAnalysisRepository;

    /**
     * DB의 모든 비디오 데이터를 엘라스틱서치로 동기화
     */
    @Async
    @Transactional(readOnly = true)
    public void syncAllVideosToElasticsearch() {
        log.info("[ES Sync] DB에서 비디오 메타데이터 조회를 시작합니다...");

        int page = 0;
        int chunkSize = 1000;
        boolean hasNext = true;
        int totalSynced = 0;

        while (hasNext) {
            // findAll 대신 새로 만든 findSliceBy 호출 (Page -> Slice로 변경)
            Slice<VideoMetadata> videoSlice = videoMetadataRepository.findAllSliceBy(PageRequest.of(page, chunkSize));

            if (videoSlice.isEmpty()) break;

            // 1. 메타데이터 ID 목록 추출 (태그 조회용)
            List<Long> metadataIds = videoSlice.getContent().stream()
                    .map(VideoMetadata::getId)
                    .toList();

            // 2. 비디오 외부 ID 목록 추출 (AI 데이터 조회용)
            List<Long> videoIds = videoSlice.getContent().stream()
                    .map(VideoMetadata::getVideoId)
                    .toList();

            // 부모 태그까지 FETCH JOIN 하는 쿼리 사용 (N+1 해결)
            List<VideoTag> allTagsForChunk = videoTagRepository.findWithTagAndParentByVideoMetadataIdIn(metadataIds);

            Map<Long, List<VideoTag>> tagsByMetadataId = allTagsForChunk.stream()
                    .collect(Collectors.groupingBy(vt -> vt.getVideoMetadata().getId()));

            List<VideoAiAnalysis> aiAnalysisChunk = videoAiAnalysisRepository.findAllById(videoIds);
            Map<Long, VideoAiAnalysis> aiAnalysisByVideoId = aiAnalysisChunk.stream()
                    .collect(Collectors.toMap(VideoAiAnalysis::getId, Function.identity()));

            // 비디오 엔티티를 ES용 문서로 변환하면서 matchedTags 계산
            List<VideoDocument> documents = videoSlice.stream().map(video -> {

                List<VideoTag> vTags = tagsByMetadataId.getOrDefault(video.getId(), List.of());

                Map<TagSource, Set<String>> tagsBySource = vTags.stream()
                        .collect(Collectors.groupingBy(VideoTag::getSource,
                                Collectors.flatMapping(vt -> vt.getTag().getParent() != null
                                                ? Stream.of(vt.getTag().getTagName(), vt.getTag().getParent().getTagName())
                                                : Stream.of(vt.getTag().getTagName()),
                                        Collectors.toSet())));

                Set<String> userTags = tagsBySource.getOrDefault(TagSource.USER, Set.of());
                Set<String> aiTags = tagsBySource.getOrDefault(TagSource.AI, Set.of());

                // 3. 교집합 추출
                List<String> matchedTags = new ArrayList<>(userTags);
                matchedTags.retainAll(aiTags);

                // 4. 합집합 추출
                Set<String> allTagsSet = new HashSet<>(userTags);
                allTagsSet.addAll(aiTags);
                List<String> distinctAllTags = new ArrayList<>(allTagsSet);

                VideoAiAnalysis aiAnalysis = aiAnalysisByVideoId.get(video.getVideoId());

                // 5. 4개의 파라미터를 정확히 전달!
                return VideoDocument.from(video, distinctAllTags, matchedTags, aiAnalysis);

            }).toList();

            // Bulk Insert
            videoSearchRepository.saveAll(documents);
            totalSynced += documents.size();

            log.info("[ES Sync] {}번째 페이지({}건) 인덱싱 완료...", page, documents.size());

            hasNext = videoSlice.hasNext();
            page++;
        }

        log.info("[ES Sync] 총 {}개의 비디오 데이터가 엘라스틱서치에 성공적으로 인덱싱되었습니다! 🎉", totalSynced);
    }
}