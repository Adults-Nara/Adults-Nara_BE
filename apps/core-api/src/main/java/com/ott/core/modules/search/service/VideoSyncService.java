package com.ott.core.modules.search.service;

import com.ott.common.persistence.entity.VideoAiAnalysis;
import com.ott.common.persistence.entity.VideoMetadata;
import com.ott.common.persistence.entity.VideoTag;
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


import java.util.List;
import java.util.Map;
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

            // 부모 태그를 포함하도록 Grouping 로직 완벽 수정
            Map<Long, List<String>> tagsByVideoId = allTagsForChunk.stream()
                    .collect(Collectors.groupingBy(
                            vt -> vt.getVideoMetadata().getId(),
                            Collectors.mapping(VideoTag::getTag, Collectors.toList())
                    ))
                    .entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> e.getValue().stream()
                                    .flatMap(tag -> tag.getParent() != null
                                            ? Stream.of(tag.getTagName(), tag.getParent().getTagName())
                                            : Stream.of(tag.getTagName()))
                                    .distinct()
                                    .toList()
                    ));

            List<VideoAiAnalysis> aiAnalysisChunk = videoAiAnalysisRepository.findAllById(videoIds);
            Map<Long, VideoAiAnalysis> aiAnalysisByVideoId = aiAnalysisChunk.stream()
                    .collect(Collectors.toMap(VideoAiAnalysis::getId, Function.identity()));
            // 비디오 엔티티를 ES용 문서로 변환
            List<VideoDocument> documents = videoSlice.stream().map(video -> {

                // 미리 만들어둔 메모리 맵(tagsByVideoId)에서 ID로 태그를 꺼낸다.
                List<String> tagNames = tagsByVideoId.getOrDefault(video.getId(), java.util.List.of());
                VideoAiAnalysis aiAnalysis = aiAnalysisByVideoId.get(video.getVideoId());
                return VideoDocument.from(video, tagNames, aiAnalysis);

            }).toList();

            // Bulk Insert
            videoSearchRepository.saveAll(documents);
            totalSynced += documents.size();

            log.info("[ES Sync] {}번째 페이지({}건) 인덱싱 완료...", page, documents.size());

            // 다음 페이지가 있는지 확인하고 넘어가기
            hasNext = videoSlice.hasNext();
            page++;
        }

        log.info("[ES Sync] 총 {}개의 비디오 데이터가 엘라스틱서치에 성공적으로 인덱싱되었습니다! 🎉", totalSynced);
    }
}