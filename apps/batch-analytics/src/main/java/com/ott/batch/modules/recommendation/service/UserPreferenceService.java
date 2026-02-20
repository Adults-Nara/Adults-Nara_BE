package com.ott.batch.modules.recommendation.service;

import com.ott.batch.modules.User.UserPreferenceRepository;
import com.ott.batch.modules.recommendation.repository.VideoTagRepository;
import com.ott.common.persistence.entity.Tag;
import com.ott.common.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
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
    private final RedisTemplate<String, String> redisTemplate; // Key: String, Value: TagName(String)

    @Async("watchHistoryTaskExecutor") // 별도 스레드에서 실행 (메인 로직 방해 X)
    @Transactional
    public void reflectWatchScore(Long userId, Long videoId, Integer watchSeconds, boolean isCompleted) {

        // 1. 영상 태그 조회
        List<Tag> tags = videoTagRepository.findTagsByVideoId(videoId);
        if (tags.isEmpty()) return;

        // 2. 점수 계산
        double scoreToAdd = calculateScore(watchSeconds, isCompleted);
        if (scoreToAdd <= 0) return;

        // 3. Redis & DB 업데이트
        LocalDateTime now = LocalDateTime.now();
        String redisKey = "user:" + userId + ":preference"; // Key 예시: user:1:preference

        for (Tag tag : tags) {
            // [Redis ZSet] 태그 점수 누적 (ZINCRBY) 자동으로 점수가 높은 순서대로 정렬됨
            redisTemplate.opsForZSet().incrementScore(redisKey, tag.getTagName(), scoreToAdd);

            userPreferenceRepository.addScore(
                    IdGenerator.generate(),
                    userId,
                    tag.getId(),
                    scoreToadd,
                    now
            );
        }
        log.info("Preference Updated (Redis+DB) - userId: {}, videoId: {}, score: {}", userId, videoId, scoreToAdd);
    }
}
