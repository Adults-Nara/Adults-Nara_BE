package com.ott.core.modules.bookmark.service;

import com.ott.common.persistence.entity.VideoMetadata;
import com.ott.core.modules.bookmark.dto.RankingResponse;
import com.ott.core.modules.video.repository.VideoMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingService {

    private final StringRedisTemplate stringRedisTemplate;
    private final VideoMetadataRepository videoMetadataRepository;

    private static final String KEY_RANKING = "video:ranking";

    @Transactional(readOnly = true)
    public List<RankingResponse> getTop10Videos() {
        // Redis ZSet에서 상위 10개 ID와 점수(Score)를 내림차순으로 가져옴 (0등 ~ 9등)
        Set<ZSetOperations.TypedTuple<String>> top10Tuples =
                stringRedisTemplate.opsForZSet().reverseRangeWithScores(KEY_RANKING, 0, 9);

        // 랭킹 데이터가 없으면 빈 리스트 반환
        if (top10Tuples == null || top10Tuples.isEmpty()) {
            return Collections.emptyList();
        }

        // 빠른 조회를 위해 Redis에서 꺼낸 데이터를 Map과 List로 분리
        List<Long> videoIds = new ArrayList<>();
        Map<Long, Double> scoreMap = new HashMap<>(); // videoId -> 점수
        Map<Long, Integer> rankMap = new HashMap<>(); // videoId -> 순위(1, 2, 3...)

        int currentRank = 1;
        for (ZSetOperations.TypedTuple<String> tuple : top10Tuples) {
            Long videoId = Long.valueOf(tuple.getValue());
            Double score = tuple.getScore() != null ? tuple.getScore() : 0.0;

            videoIds.add(videoId);
            scoreMap.put(videoId, score);
            rankMap.put(videoId, currentRank++);
        }

        // DB에서 메타데이터 일괄 조회
        List<VideoMetadata> metadataList = videoMetadataRepository.findAllByVideoIdIn(videoIds);

        // 애플리케이션 메모리 단에서 Redis가 알려준 순위 재정렬
        return metadataList.stream()
                .map(meta -> RankingResponse.of(
                        rankMap.get(meta.getVideoId()), // Map에서 순위 꺼내오기
                        meta,
                        scoreMap.get(meta.getVideoId()) // Map에서 실시간 점수 꺼내오기
                ))
                // DTO에 부여된 rank(순위) 필드를 기준으로 오름차순(1위 -> 10위) 정렬
                .sorted(Comparator.comparingInt(RankingResponse::getRank))
                .collect(Collectors.toList());
    }
}