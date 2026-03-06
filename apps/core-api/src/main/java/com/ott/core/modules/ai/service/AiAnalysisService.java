package com.ott.core.modules.ai.service;

import com.ott.common.persistence.entity.Tag;
import com.ott.common.persistence.entity.VideoAiAnalysis;
import com.ott.common.persistence.entity.VideoMetadata;
import com.ott.common.persistence.entity.VideoTag;
import com.ott.common.persistence.enums.TagSource;
import com.ott.core.modules.ai.dto.VideoAiAnalysisCompletedEvent;
import com.ott.core.modules.ai.repository.VideoAiAnalysisRepository;
import com.ott.core.modules.search.event.VideoIndexRequestedEvent;
import com.ott.core.modules.tag.repository.TagRepository;
import com.ott.core.modules.tag.repository.VideoTagRepository;
import com.ott.core.modules.video.repository.VideoMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAnalysisService {

    private final VideoMetadataRepository videoMetadataRepository;
    private final VideoAiAnalysisRepository videoAiAnalysisRepository;
    private final TagRepository tagRepository;
    private final VideoTagRepository videoTagRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void processAnalysisResult(VideoAiAnalysisCompletedEvent event) {
        VideoMetadata metadata = videoMetadataRepository.findByVideoId(event.videoId())
                .orElseThrow(
                        () -> new IllegalArgumentException("VideoMetadata not found for videoId: " + event.videoId()));

        // 1. AI 태그 저장 (Tag 테이블에 없으면 생성, source = AI)
        List<String> aiTagNames = event.aiTags();
        if (aiTagNames != null && !aiTagNames.isEmpty()) {
            List<Tag> existingTags = tagRepository.findAll(); // TODO: Inefficient, better to query by IN clause, but
                                                              // TagRepository lacks findByTagNameIn

            for (String tagName : aiTagNames) {
                Tag tag = existingTags.stream()
                        .filter(t -> t.getTagName().equals(tagName))
                        .findFirst()
                        .orElseGet(() -> {
                            Tag newTag = new Tag(tagName);
                            return tagRepository.save(newTag);
                        });

                // VideoTag 생성 (source = AI)
                // 중복 방지 로직 (옵션)
                boolean alreadyLinked = videoTagRepository.findTagsByVideoMetadataId(metadata.getId())
                        .stream().anyMatch(t -> t.getId().equals(tag.getId()));

                if (!alreadyLinked) {
                    VideoTag videoTag = new VideoTag(metadata, tag, TagSource.AI);
                    videoTagRepository.save(videoTag);
                }
            }
        }

        // 2. VideoAiAnalysis 엔티티 저장 (요약, 자막, 임베딩)
        videoAiAnalysisRepository.findByVideoId(metadata.getVideoId())
                .ifPresentOrElse(
                        existing -> log.info("이미 AI 분석 결과가 존재합니다. 덮어쓰지 않습니다. videoMetadataId: {}", metadata.getId()),
                        () -> {
                            VideoAiAnalysis analysis = VideoAiAnalysis.builder()
                                    .id(metadata.getVideoId())
                                    .summary(event.summary())
                                    .subtitleUrl(event.subtitleUrl())
                                    .embedding(event.embedding())
                                    .build();
                            videoAiAnalysisRepository.save(analysis);
                        });

        // 3. (Phase 3 연계) ES 문서를 업데이트하도록 내부 이벤트 발행
        // AI 데이터가 DB에 반영되었으므로 다시 인덱싱하도록 트리거
        eventPublisher.publishEvent(new VideoIndexRequestedEvent(event.videoId()));
    }
}
