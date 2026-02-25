package com.ott.core.modules.interaction.service;

import com.ott.common.error.BusinessException;
import com.ott.common.error.ErrorCode;
import com.ott.common.persistence.entity.*;
import com.ott.common.persistence.enums.InteractionType;
import com.ott.core.modules.interaction.repository.InteractionRepository;
import com.ott.core.modules.preference.event.InteractionEvent;
import com.ott.core.modules.user.repository.UserRepository;
import com.ott.core.modules.video.repository.VideoMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class InteractionService {

    private final InteractionRepository interactionRepository;
    private final UserRepository userRepository;
    private final VideoMetadataRepository videoMetadataRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final StringRedisTemplate stringRedisTemplate;

    public void interact(Long userId, Long videoId, InteractionType newType) {
        User user = findUser(userId);
        VideoMetadata metadata = findMetadataByVideoId(videoId);
        Long metadataId = metadata.getId();

        Optional<Interaction> existingInteraction = interactionRepository.findByUserAndVideoMetadata(user, metadata);

        try {
            if (existingInteraction.isPresent()) {
                Interaction interaction = existingInteraction.get();
                InteractionType oldType = interaction.getInteractionType();

                if (oldType == newType) {
                    // 같은 버튼 또 누름 -> 취소
                    interactionRepository.delete(interaction);
                    interactionRepository.flush(); // 따닥 방어
                    updateRedis(videoId, oldType, -1);
                    // [이벤트 발행] 취소되었으므로 newType은 null로 보냄
                    eventPublisher.publishEvent(new InteractionEvent(userId, metadataId, oldType, null));
                } else {
                    // 변경
                    interaction.changeType(newType);
                    interactionRepository.save(interaction);
                    interactionRepository.flush(); // 따닥 방어

                    updateRedis(videoId, oldType, -1); // 기존 카운트 감소
                    updateRedis(videoId, newType, 1);  // 새 카운트 증가
                    // [이벤트 발행] 변경 전/후 타입 모두 보냄
                    eventPublisher.publishEvent(new InteractionEvent(userId, metadataId, oldType, newType));
                }
            } else {
                // 생성
                Interaction newInteraction = new Interaction(user, metadata, newType);
                interactionRepository.save(newInteraction);
                interactionRepository.flush(); // 따닥 방어
                updateRedis(videoId, newType, 1);
                // [이벤트 발행] 새로 생성되었으므로 oldType은 null로 보냄
                eventPublisher.publishEvent(new InteractionEvent(userId, metadataId, null, newType));
                }
            } catch (DataIntegrityViolationException e) {
                // [동시성 제어] DB 유니크 제약 조건 위반 시 Redis 카운트 꼬임 방지
                log.warn("[Interaction] 동시 요청으로 인한 중복 반영 방어 - userId: {}, videoId: {}", userId, videoId);
                throw new BusinessException(ErrorCode.INTERACTION_CONFLICT);
            }
        }

    // 조회
    @Transactional(readOnly = true)
    public Optional<InteractionType> getInteractionStatus(Long userId, Long videoId) {
        return interactionRepository.findByUserIdAndVideoId(userId, videoId)
                .map(Interaction::getInteractionType);
    }

    /**
     * Redis 카운트, 랭킹, 그리고 스케줄러 동기화 큐(Dirty Set) 업데이트
     */
    private void updateRedis(Long videoId, InteractionType type, int delta) {
        String videoIdStr = String.valueOf(videoId);
        String typeLower = type.name().toLowerCase(); // like, dislike, superlike

        // 개별 카운트 증감 (Hash) - 상세 페이지 표시용
        String countKey = "video:count:" + typeLower;
        stringRedisTemplate.opsForHash().increment(countKey, videoIdStr, delta);


        // 스케줄러 처리 대상 목록에 추가 (Set) - Write-Back 패턴
        String dirtyKey = "video:dirty:" + typeLower;
        stringRedisTemplate.opsForSet().add(dirtyKey, videoIdStr);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private VideoMetadata findMetadataByVideoId(Long videoId) {
        return videoMetadataRepository.findByVideoId(videoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VIDEO_NOT_FOUND));
    }
}