package com.ott.core.modules.preference.service;

import com.ott.common.persistence.entity.Tag;
import com.ott.common.persistence.entity.UserPreference;
import com.ott.common.persistence.enums.InteractionType;
import com.ott.common.util.IdGenerator;
import com.ott.core.modules.preference.dto.TagScoreDto;
import com.ott.core.modules.preference.repository.UserPreferenceRepository;
import com.ott.core.modules.tag.repository.VideoTagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserPreferenceService {

    private final UserPreferenceRepository userPreferenceRepository;
    private final VideoTagRepository videoTagRepository;
    private final StringRedisTemplate stringRedisTemplate;

    private static final double SCORE_SUPERLIKE = 5.0; // 왕따봉
    private static final double SCORE_LIKE = 4.0;      // 좋아요
    private static final double SCORE_DISLIKE = -5.0;      // 좋아요
    private static final double SCORE_COMPLETION = 3.0;
    private static final double SCORE_PER_MINUTE = 0.1;

    /**
     * [핵심] 시청 기록 기반 취향 분석 (비동기 처리)
     * - Redis ZSet에 점수 반영 (실시간 추천용)
     * - DB에 점수 누적 저장 (데이터 보존용)
     */

    @Transactional
    public void reflectWatchScore(Long userId, Long videoId, Integer watchSeconds, boolean isCompleted) {
        double scoreToAdd = 0.0;

        if (watchSeconds != null && watchSeconds > 0) {
            scoreToAdd += (watchSeconds / 60.0) * SCORE_PER_MINUTE;
        }
        if (isCompleted) {
            scoreToAdd += SCORE_COMPLETION;
        }

        if (scoreToAdd > 0) {
            updateScores(userId, videoId, scoreToAdd);
            log.info("[Preference] 시청 점수 반영 완료 - userId: {}, videoId: {}, score: {}", userId, videoId, scoreToAdd);
        }
    }


    /**
     * [인터랙션] 좋아요 클릭 시 점수 반영
     */
    @Transactional
    public void reflectInteractionScore(Long userId, Long videoId, InteractionType oldType, InteractionType newType) {

        double oldScore = getScoreByType(oldType);
        double newScore = getScoreByType(newType);

        // 2. 차이값(Delta) 계산
        // 예: 좋아요(4) -> 슈퍼라이크(5) = 1점 추가
        // 예: 좋아요(4) -> 취소(0) = -4점 추가 (차감됨)
        double delta = newScore - oldScore;

        // 3. 차이값이 0이 아닐 때만 Redis와 DB 업데이트 실행
        if (delta != 0.0) {
            updateScores(userId, videoId, delta);
            log.info("[Preference] 인터랙션 점수 차이값 반영 완료 - userId: {}, videoId: {}, delta: {}", userId, videoId, delta);
        }
    }

    private void updateScores(Long userId, Long videoId, Double score) {
        // 1. 영상의 태그 목록 조회
        List<Tag> tags = videoTagRepository.findTagsByVideoId(videoId);
        if (tags.isEmpty()) return;

        String redisKey = "user:" + userId + ":preference";
        LocalDateTime now = LocalDateTime.now();

        for (Tag tag : tags) {
            // [RDBMS] Native Upsert 쿼리: 영구 저장용 백업
            userPreferenceRepository.addScore(IdGenerator.generate(), userId, tag.getId(), score, now);

            // [Redis] ZINCRBY: 실시간 태그 랭킹 업데이트
            stringRedisTemplate.opsForZSet().incrementScore(redisKey, tag.getTagName(), score);
        }
    }

    private double getScoreByType(InteractionType type) {
        if (type == null) return 0.0;

        return switch (type) {
            case SUPERLIKE -> SCORE_SUPERLIKE;
            case LIKE -> SCORE_LIKE;
            case DISLIKE -> SCORE_DISLIKE;
        };
    }
    /**
     * [조회 API] 유저의 Top N 취향 태그 가져오기 (Cache-Aside 패턴 적용)
     */
    @Transactional(readOnly = true)
    public List<TagScoreDto> getTopPreferences(Long userId, int limit) {
        String redisKey = "user:" + userId + ":preference";

        // 1. [Cache Hit] Redis에서 먼저 조회 (1등부터 한도(limit)까지 가져옴)
        // 0은 1등, limit - 1은 N등을 의미합니다.
        var redisResult = stringRedisTemplate.opsForZSet().reverseRangeWithScores(redisKey, 0, limit - 1);

        if (redisResult != null && !redisResult.isEmpty()) {
            log.info("[Preference Read] Redis 캐시 히트! 초고속 반환 - userId: {}", userId);
            return redisResult.stream()
                    .map(tuple -> new TagScoreDto(tuple.getValue(), tuple.getScore()))
                    .toList();
        }

        // 2. [Cache Miss] Redis에 데이터가 텅 비었음! DB에서 조회 시작
        log.warn("[Preference Read] Redis 캐시 미스. DB에서 조회 및 자가 복구 진행 - userId: {}", userId);
        List<UserPreference> dbPreferences = userPreferenceRepository.findWithTagByUserId(userId);

        if (dbPreferences.isEmpty()) {
            log.info("[Preference Read] 취향 데이터가 없는 신규 유저입니다 - userId: {}", userId);
            return List.of(); // 빈 리스트 반환 (나중에 ES에서 기본 인기 영상 노출용으로 쓰임)
        }

        // 3. [Self-Healing] DB에서 가져온 데이터를 다시 Redis에 채워 넣기 (복구)
        List<TagScoreDto> recoveredScores = new java.util.ArrayList<>();

        for (UserPreference pref : dbPreferences) {
            String tagName = pref.getTag().getTagName();
            Double score = pref.getScore();

            // Redis에 다시 ZADD
            stringRedisTemplate.opsForZSet().add(redisKey, tagName, score);
            recoveredScores.add(new TagScoreDto(tagName, score));
        }

        // 4. 복구된 데이터를 점수 내림차순으로 정렬해서 요청한 개수(limit)만큼만 짤라서 반환
        return recoveredScores.stream()
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .limit(limit)
                .toList();
    }
}