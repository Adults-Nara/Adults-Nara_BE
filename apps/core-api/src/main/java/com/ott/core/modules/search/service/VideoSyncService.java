package com.ott.core.modules.search.service;

import com.ott.common.persistence.entity.Tag;
import com.ott.common.persistence.entity.VideoMetadata;
import com.ott.core.modules.search.document.VideoDocument;
import com.ott.core.modules.search.repository.VideoSearchRepository;
import com.ott.core.modules.tag.repository.VideoTagRepository;
import com.ott.core.modules.video.repository.VideoMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoSyncService {

        private final VideoMetadataRepository videoMetadataRepository;
        private final VideoTagRepository videoTagRepository;
        private final VideoSearchRepository videoSearchRepository;

        /**
         * DB의 모든 비디오 데이터를 엘라스틱서치로 동기화
         */
        @Transactional(readOnly = true)
        public void syncAllVideosToElasticsearch() {
            log.info("[ES Sync] DB에서 비디오 메타데이터 조회를 시작합니다...");

            // 1. DB에서 모든 비디오를 가져옵니다.
            List<VideoMetadata> allVideos = videoMetadataRepository.findAll();

            // 2. 비디오 엔티티를 ES용 문서(Document)로 변환합니다.
            List<VideoDocument> documents = allVideos.stream().map(video -> {

                // 2-1. 비디오에 매핑된 태그 '이름'만 리스트로 뽑아냅니다. (예: ["SF", "액션"])
                List<String> tagNames = videoTagRepository.findTagsByVideoId(video.getId())
                        .stream()
                        .map(Tag::getTagName)
                        .toList();

                // 2-2. ES 전용 객체로 조립합니다.
                return VideoDocument.builder()
                        .id(video.getId())
                        .title(video.getTitle())
                        .description(video.getDescription())
                        .tags(tagNames)
                        .viewCount(video.getViewCount())
                        .likeCount(video.getLikeCount())
                        .createdAt(video.getCreatedAt().toString()) // 날짜를 안전한 문자열
                        .videoType(video.getVideoType() != null ? video.getVideoType().name() : "NONE")
                        .build();
            }).toList();

            // 3. 엘라스틱서치에 통째로 벌크(Bulk) 저장
            videoSearchRepository.saveAll(documents);

            log.info("[ES Sync] 총 {}개의 비디오 데이터가 엘라스틱서치에 성공적으로 인덱싱되었습니다! 🎉", documents.size());
        }

}
