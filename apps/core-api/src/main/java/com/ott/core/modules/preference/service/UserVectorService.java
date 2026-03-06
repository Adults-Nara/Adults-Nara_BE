package com.ott.core.modules.preference.service;

import com.ott.common.persistence.enums.InteractionType;
import com.ott.core.modules.search.document.VideoDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserVectorService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final RedisTemplate<String, List<Double>> redisVectorTemplate;

    private static final String REDIS_USER_VECTOR_KEY_PREFIX = "user:vector:";
    private static final int VECTOR_DIMENSION = 384;

    private static final double ALPHA_WATCH_NORMAL = 0.05;    // 일반 시청: 5% 반영
    private static final double ALPHA_WATCH_COMPLETE = 0.10;  // 완료: 10% 반영
    private static final double ALPHA_LIKE = 0.20;            // 좋아요: 20% 반영
    private static final double ALPHA_SUPERLIKE = 0.35;       // 왕따봉: 35% 반영

    public List<Double> getUserVector(Long userId) {
        String redisKey = REDIS_USER_VECTOR_KEY_PREFIX + userId;
        try {
            return redisVectorTemplate.opsForValue().get(redisKey);
        } catch (Exception e) {
            log.error("[UserVector] Redis에서 유저 벡터 조회 실패 (Fallback 동작) - userId: {}", userId, e);
            return null; // 에러 발생 시 null을 반환하여 기본 인기 피드로 우회하도록 함
        }
    }

    public void updateVectorFromWatch(Long userId, Long videoId, boolean isCompleted) {
        double alpha = isCompleted ? ALPHA_WATCH_COMPLETE : ALPHA_WATCH_NORMAL;
        processVectorUpdate(userId, videoId, alpha);
    }

    public void updateVectorFromInteraction(Long userId, Long videoId, InteractionType type) {
        double alpha = (type == InteractionType.SUPERLIKE) ? ALPHA_SUPERLIKE : ALPHA_LIKE;
        processVectorUpdate(userId, videoId, alpha);
    }

    private void processVectorUpdate(Long userId, Long videoId, double alpha) {
        try {
            // 1. ES에서 시청/인터랙션 한 영상의 벡터를 조회
            VideoDocument videoDoc = elasticsearchOperations.get(videoId.toString(), VideoDocument.class);
            if (videoDoc == null || videoDoc.getEmbedding() == null || videoDoc.getEmbedding().isEmpty()) {
                log.warn("[UserVector] 영상의 벡터 데이터가 없어 업데이트 취소 - videoId: {}", videoId);
                return;
            }
            List<Double> videoVector = videoDoc.getEmbedding();

            // 2. Redis에서 유저의 기존 벡터를 조회
            String redisKey = REDIS_USER_VECTOR_KEY_PREFIX + userId;
            List<Double> currentUserVector = redisVectorTemplate.opsForValue().get(redisKey);

            List<Double> newVector;
            if (currentUserVector == null || currentUserVector.isEmpty()) {
                // 신규 유저의 첫 상호작용인 경우: 현재 영상 벡터가 곧 유저의 취향 벡터가 됨
                newVector = videoVector;
                log.info("[UserVector] 신규 유저 취향 벡터 최초 생성 - userId: {}", userId);
            } else {
                // 기존 취향이 있는 경우: EMA(지수 이동 평균) 방식으로 벡터 융합
                newVector = calculateEMA(currentUserVector, videoVector, alpha);
            }

            // 3. 업데이트된 벡터를 Redis에 저장 (예: TTL 30일 설정)
            redisVectorTemplate.opsForValue().set(redisKey, newVector, 30, TimeUnit.DAYS);
            log.info("[UserVector] 유저 취향 벡터 업데이트 완료 - userId: {}, alpha: {}", userId, alpha);

        } catch (Exception e) {
            log.error("[UserVector] 벡터 업데이트 중 오류 발생 - userId: {}, videoId: {}", userId, videoId, e);
        }
    }

    /**
     * 지수 이동 평균(EMA)을 활용한 벡터 융합 계산
     * V_new = (1 - alpha) * V_old + (alpha) * V_video
     */
    private List<Double> calculateEMA(List<Double> oldVector, List<Double> newVector, double alpha) {
        if (oldVector.size() != VECTOR_DIMENSION || newVector.size() != VECTOR_DIMENSION) {
            throw new IllegalArgumentException("벡터의 차원이 일치하지 않습니다. 기대값: " + VECTOR_DIMENSION);
        }
        List<Double> updatedVector = new ArrayList<>(VECTOR_DIMENSION);
        for (int i = 0;i < VECTOR_DIMENSION; i++) {
            double oldVal = oldVector.get(i);
            double newVal = newVector.get(i);

            // 수학 공식 적용
            double blendedVal = ((1.0 - alpha) * oldVal) + (alpha * newVal);
            updatedVector.add(blendedVal);
        }
        return updatedVector;
    }
}
