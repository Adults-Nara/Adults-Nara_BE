package com.ott.core.modules.search.service;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.ott.common.persistence.entity.User;
import com.ott.common.persistence.entity.WatchHistory;
import com.ott.common.persistence.enums.VideoType;
import com.ott.core.modules.search.document.VideoDocument;
import com.ott.core.modules.search.dto.VideoSearchResponse;
import com.ott.core.modules.user.repository.UserRepository;
import com.ott.core.modules.watch.repository.WatchHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VideoSearchService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final UserRepository userRepository;
    private final WatchHistoryRepository watchHistoryRepository;

    /**
     * 1. 메인 검색 API (형태소 분석 + 필터링 + 오타 보정)
     */
    public Page<VideoSearchResponse> searchVideos(Long currentUserId, String keyword, VideoType videoType, String tag, Pageable pageable) {

        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        // [필수 필터] 삭제되지 않은 영상만 노출
        boolQueryBuilder.filter(f -> f.term(t -> t.field("deleted").value(false)));

        // [MUST] 검색어 매칭 (제목 3배 가중치, 내용은 1배)
        if (keyword != null && !keyword.isBlank()) {
            boolQueryBuilder.must(m -> m
                    .multiMatch(match -> match
                            .query(keyword)
                            .fields("title^3.0", "description") // Nori 분석기가 작동하는 필드
                            .fuzziness("AUTO")
                    )
            );
        }

        // [FILTER] 비디오 타입 필터 (예: SHORT, NORMAL) - 캐싱 적용되어 매우 빠름
        if (videoType != null) {
            boolQueryBuilder.filter(f -> f.term(t -> t.field("videoType").value(videoType.name())));
        }

        // [FILTER] 태그 필터 (정확히 일치하는 태그)
        if (tag != null && !tag.isBlank()) {
            boolQueryBuilder.filter(f -> f.term(t -> t.field("tags").value(tag)));
        }

        // 쿼리 조립
        Query query = Query.of(q -> q.bool(boolQueryBuilder.build()));

        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(query)
                .withPageable(pageable) // 페이징 처리
                .build();

        // ES에 쿼리 실행
        SearchHits<VideoDocument> searchHits = elasticsearchOperations.search(nativeQuery, VideoDocument.class);
        List<VideoDocument> documents = searchHits.getSearchHits().stream().map(SearchHit::getContent).toList();

        if (documents.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, searchHits.getTotalHits());
        }

        // 2. 검색된 영상들의 업로더 ID와 비디오 ID 추출
        Set<Long> uploaderIds = documents.stream().map(VideoDocument::getUserId).collect(Collectors.toSet());
        List<Long> videoIds = documents.stream().map(VideoDocument::getVideoId).toList();
        // 3. 업로더 프로필 정보 한 번에(IN 쿼리) 조회
        Map<Long, User> uploaderMap = userRepository.findAllById(uploaderIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 4. 로그인한 유저라면 시청 이력 한 번에(IN 쿼리) 조회
        Map<Long, Integer> progressMap = new HashMap<>();
        if (currentUserId != null) {
            // (주의: repository에 findByUserIdAndVideoIdIn 메서드가 있다고 가정)
            List<WatchHistory> histories = watchHistoryRepository.findByUserIdAndVideoMetadata_VideoIdIn(currentUserId, videoIds);
            for (WatchHistory wh : histories) {
                Integer duration = wh.getVideoMetadata().getDuration();
                Integer lastPos = wh.getLastPosition();
                if (duration != null && duration > 0 && lastPos != null) {
                    int progress = (int) Math.min(100.0, Math.round(((double) lastPos / duration) * 100));
                    progressMap.put(wh.getVideoMetadata().getVideoId(), progress);
                }
            }
        }
        // 5. 뼈대(Document) + 살(Uploader, Progress) 합치기!
        List<VideoSearchResponse> content = documents.stream().map(doc -> {
            User uploader = uploaderMap.get(doc.getUserId());
            String uploaderName = (uploader != null) ? uploader.getNickname() : "알 수 없음";
            String profileUrl = (uploader != null) ? uploader.getProfileImageUrl() : "";
            int progress = progressMap.getOrDefault(doc.getVideoId(), 0);
            return VideoSearchResponse.of(doc, uploaderName, profileUrl, progress);
        }).collect(Collectors.toList());

        return new PageImpl<>(content, pageable, searchHits.getTotalHits());
    }

    /**
     * 2. 실시간 자동완성 API (Edge N-gram 활용)
     * 사용자가 타이핑할 때마다 즉각적으로 연관 검색어(제목) 5개를 내려줍니다.
     */
    public List<String> autocomplete(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
        boolQueryBuilder.filter(f -> f.term(t -> t.field("deleted").value(false)));
        // 초성 검사
        boolean isOnlyChosung = keyword.matches("^[ㄱ-ㅎㄲㄸㅃㅆㅉ\\s]+$");

        if (isOnlyChosung) {
            boolQueryBuilder.must(m -> m
                    .prefix(p -> p
                            .field("titleChosung")
                            .value(keyword)
                    )
            );
        } else {
            boolQueryBuilder.must(m -> m
                    .match(m2 -> m2
                            .field("title.ngram")
                            .query(keyword)
                    )
            );
        }

        Query query = Query.of(q -> q.bool(boolQueryBuilder.build()));

        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(query)
                .withMaxResults(5) // 자동완성은 상위 5개만 빠르게
                .build();

        SearchHits<VideoDocument> searchHits = elasticsearchOperations.search(nativeQuery, VideoDocument.class);

        return searchHits.getSearchHits().stream()
                .map(hit -> hit.getContent().getTitle())
                .distinct() // 중복 제목 제거
                .collect(Collectors.toList());
    }
}