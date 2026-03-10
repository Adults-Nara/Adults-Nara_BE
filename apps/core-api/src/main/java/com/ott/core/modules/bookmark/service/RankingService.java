package com.ott.core.modules.bookmark.service;

import com.ott.common.persistence.entity.User;
import com.ott.common.persistence.entity.VideoMetadata;
import com.ott.common.persistence.entity.WatchHistory;
import com.ott.core.modules.bookmark.dto.RankingResponse;
import com.ott.core.modules.user.repository.UserRepository;
import com.ott.core.modules.video.repository.VideoMetadataRepository;
import com.ott.core.modules.watch.repository.WatchHistoryRepository;
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
    private final UserRepository userRepository;
    private final WatchHistoryRepository watchHistoryRepository;

    private static final String KEY_RANKING_LONG = "video:ranking:long";

    /**
     * [Step 3] Redis에 저장된 카운트를 통해 실시간 인기 차트 제공
     */
    @Transactional(readOnly = true)
    public List<RankingResponse> getTopBookmarkVideos(Long currentUserId, int limit) {
        // 1. Redis에서 순위대로 꺼내옴
        Set<ZSetOperations.TypedTuple<String>> topRankings = stringRedisTemplate.opsForZSet()
                .reverseRangeWithScores(KEY_RANKING_LONG, 0, limit - 1);

        if (topRankings == null || topRankings.isEmpty()) {
            return List.of();
        }

        List<Long> rankedVideoIds = new ArrayList<>();
        Map<Long, Double> scoreMap = new HashMap<>();

        for (ZSetOperations.TypedTuple<String> tuple : topRankings) {
            String value = tuple.getValue();
            if (value != null) {
                Long videoId = Long.valueOf(value);
                rankedVideoIds.add(videoId);
                scoreMap.put(videoId, tuple.getScore());
            }
        }

        // 2. DB에서 메타데이터 긁어오기 (순서 섞임 주의)
        List<VideoMetadata> metadataList = videoMetadataRepository.findAllByVideoIdIn(rankedVideoIds);
        Map<Long, VideoMetadata> metadataMap = metadataList.stream()
                .collect(Collectors.toMap(VideoMetadata::getVideoId, m -> m));

        // 업로더 정보 일괄 조회
        Set<Long> uploaderIds = metadataList.stream()
                .map(VideoMetadata::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, User> uploaderMap = userRepository.findAllById(uploaderIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 시청 기록일괄 조회
        Map<Long, Integer> progressMap = new HashMap<>();
        if (currentUserId != null) {
            List<WatchHistory> histories = watchHistoryRepository.findByUserIdAndVideoMetadata_VideoIdIn(currentUserId, rankedVideoIds);
            for (WatchHistory wh : histories) {
                Integer duration = wh.getVideoMetadata().getDuration();
                Integer lastPos = wh.getLastPosition();
                if (duration != null && duration > 0 && lastPos != null) {
                    int progress = (int) Math.min(100.0, Math.round(((double) lastPos / duration) * 100));
                    progressMap.put(wh.getVideoMetadata().getVideoId(), progress);
                }
            }
        }
        // 3. Redis가 알려준 정확한 순서대로 재배치하여 DTO 응답
        List<RankingResponse> responseList = new ArrayList<>();
        int currentRank = 1;

        for (Long videoId : rankedVideoIds) {
            VideoMetadata metadata = metadataMap.get(videoId);
            if (metadata != null) {
                User uploader = uploaderMap.get(metadata.getUserId());
                String uploaderName = (uploader != null) ? uploader.getNickname() : "알 수 없음";
                String profileUrl = (uploader != null) ? uploader.getProfileImageUrl() : "";
                int progress = progressMap.getOrDefault(videoId, 0);

                // DTO의 of 메서드 호출
                responseList.add(RankingResponse.of(
                        currentRank++,
                        metadata,
                        scoreMap.get(videoId),
                        uploaderName,
                        profileUrl,
                        progress
                ));
            }
        }

        return responseList;
    }
}