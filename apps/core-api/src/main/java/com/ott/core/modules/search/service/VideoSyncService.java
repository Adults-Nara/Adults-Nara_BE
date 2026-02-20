package com.ott.core.modules.search.service;

import com.ott.common.persistence.entity.Tag;
import com.ott.common.persistence.entity.VideoMetadata;
import com.ott.common.persistence.entity.VideoTag;
import com.ott.core.modules.search.document.VideoDocument;
import com.ott.core.modules.search.repository.VideoSearchRepository;
import com.ott.core.modules.tag.repository.VideoTagRepository;
import com.ott.core.modules.video.repository.VideoMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

            int page = 0;
            int chunkSize = 1000; // 한 번에 1,000건씩만 가져옵니다.
            boolean hasNext = true;
            int totalSynced = 0;

            while (hasNext) {
                // findAll 대신 새로 만든 findSliceBy 호출 (Page -> Slice로 변경)
                org.springframework.data.domain.Slice<VideoMetadata> videoSlice = videoMetadataRepository.findSliceBy(PageRequest.of(page, chunkSize));

                if (videoSlice.isEmpty()) break;

                List<Long> videoIds = videoSlice.stream().map(VideoMetadata::getId).toList();

                // 2. IN 절을 써서  "단 한 번의 쿼리"로 가져온다
                List<VideoTag> allTagsForChunk = videoTagRepository.findWithTagByVideoMetadataIdIn(videoIds);

                // 3. 가져온 태그들을 자바 메모리(RAM) 상에서 비디오 ID별로 분류(Grouping)
                // 결과: { 7777: ["SF", "로맨스"], 9999: ["액션", "SF"] }
                java.util.Map<Long, List<String>> tagsByVideoId = allTagsForChunk.stream()
                        .collect(java.util.stream.Collectors.groupingBy(
                                vt -> vt.getVideoMetadata().getId(),
                                java.util.stream.Collectors.mapping(vt -> vt.getTag().getTagName(), java.util.stream.Collectors.toList())
                        ));

                // 4. 비디오 엔티티를 ES용 문서로 변환
                List<VideoDocument> documents = videoSlice.stream().map(video -> {

                    // 미리 만들어둔 메모리 맵(tagsByVideoId)에서 0.0001초 만에 빼온다.
                    List<String> tagNames = tagsByVideoId.getOrDefault(video.getId(), java.util.List.of());

                    return VideoDocument.of(video, tagNames); // 문서 조립 책임을 위임!
                }).toList();

                // 1000개 묶음을 엘라스틱서치에 벌크 저장
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
