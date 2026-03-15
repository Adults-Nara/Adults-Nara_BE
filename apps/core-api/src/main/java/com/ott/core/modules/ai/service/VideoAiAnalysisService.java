package com.ott.core.modules.ai.service;

import com.ott.common.error.BusinessException;
import com.ott.common.error.ErrorCode;
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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoAiAnalysisService {

    private final VideoMetadataRepository videoMetadataRepository;
    private final VideoAiAnalysisRepository videoAiAnalysisRepository;
    private final TagRepository tagRepository;
    private final VideoTagRepository videoTagRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void processAnalysisResult(VideoAiAnalysisCompletedEvent event) {
        VideoMetadata metadata = videoMetadataRepository.findByVideoId(event.videoId())
                .orElseThrow(() -> new BusinessException(ErrorCode.VIDEO_METADATA_NOT_FOUND));

        // 1. AI 태그 저장
        List<String> aiTagNames = event.aiTags();
        if (aiTagNames != null && !aiTagNames.isEmpty()) {
            // 해당 비디오에 이미 등록된 AI 소스 태그들을 조회 (멱등성 보장)
            List<VideoTag> existingAiVideoTags = videoTagRepository.findAllByVideoMetadataIdAndSource(metadata.getId(), TagSource.AI);
            Set<Long> existingAiTagIds = existingAiVideoTags.stream()
                    .map(vt -> vt.getTag().getId())
                    .collect(Collectors.toSet());

            List<Tag> tagsToAssign = tagRepository.findByTagNameIn(aiTagNames);

            for (Tag tag : tagsToAssign) {
                // 이미 AI 소스로 등록된 태그인 경우 스킵
                if (existingAiTagIds.contains(tag.getId())) {
                    log.info("이미 등록된 AI 태그이므로 스킵합니다: videoId={}, tagName={}", event.videoId(), tag.getTagName());
                    continue;
                }

                // VideoTag 생성 및 저장 (source = AI)
                videoTagRepository.save(new VideoTag(metadata, tag, TagSource.AI));
            }
        }

        // 2. VideoAiAnalysis 엔티티 저장 (요약, 자막, 임베딩)
        Optional<VideoAiAnalysis> analysis = videoAiAnalysisRepository.findById(metadata.getVideoId());
        if (analysis.isPresent()) {
            log.info("이미 AI 분석 결과가 존재합니다. 덮어쓰지 않습니다. videoMetadataId: {}", metadata.getId());
        } else {
            videoAiAnalysisRepository.save(VideoAiAnalysis.builder()
                    .id(metadata.getVideoId())
                    .summary(event.summary())
                    .subtitleUrl(event.subtitleUrl())
                    .embedding(event.embedding())
                    .build());
        }

        // 3. ES 문서를 업데이트하도록 내부 이벤트 발행
        // AI 데이터가 DB에 반영되었으므로 다시 인덱싱하도록 트리거
        eventPublisher.publishEvent(new VideoIndexRequestedEvent(event.videoId()));
    }
}
