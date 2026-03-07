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
                .orElseThrow(() -> new BusinessException(ErrorCode.VIDEO_METADATA_NOT_FOUND));

        // 1. AI 태그 저장 (Tag 테이블에 없으면 생성, source = AI)
        List<String> aiTagNames = event.aiTags();
        if (aiTagNames != null && !aiTagNames.isEmpty()) {
            // 필요한 태그만 DB에서 1번에 조회 (OOM 방지)
            List<Tag> existingTags = tagRepository.findByTagNameIn(aiTagNames);

            // 영상에 이미 달린 태그 목록을 for 문 바깥에서 1번만 캐싱 (N+1 방지)
            List<Long> alreadyLinkedTagIds = videoTagRepository.findTagsByVideoMetadataId(metadata.getId())
                    .stream()
                    .map(Tag::getId)
                    .toList();

            for (String tagName : aiTagNames) {
                Tag tag = existingTags.stream()
                        .filter(t -> t.getTagName().equals(tagName))
                        .findFirst()
                        .orElseThrow(() -> new BusinessException(ErrorCode.TAG_NOT_FOUND));

                // VideoTag 생성 (source = AI)
                // 미리 조회해둔 캐시 리스트 이용 (DB 쿼리 X)
                if (!alreadyLinkedTagIds.contains(tag.getId())) {
                    VideoTag videoTag = new VideoTag(metadata, tag, TagSource.AI);
                    videoTagRepository.save(videoTag);
                }
            }
        }

        // 2. VideoAiAnalysis 엔티티 저장 (요약, 자막, 임베딩)
        Optional<VideoAiAnalysis> analysis = videoAiAnalysisRepository.findByVideoId(metadata.getVideoId());
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
