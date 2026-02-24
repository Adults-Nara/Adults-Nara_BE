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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class InteractionService {

    private final InteractionRepository interactionRepository;
    private final UserRepository userRepository;
    private final VideoMetadataRepository videoMetadataRepository;
    private final ApplicationEventPublisher eventPublisher;

    public void interact(Long userId, Long videoId, InteractionType newType) {
        User user = findUser(userId);
        VideoMetadata metadata = findMetadataByVideoId(videoId);
        Long metadataId = metadata.getId();

        Optional<Interaction> existingInteraction = interactionRepository.findByUserAndVideoMetadata(user, metadata);

            if (existingInteraction.isPresent()) {
                Interaction interaction = existingInteraction.get();
                InteractionType oldType = interaction.getInteractionType();

                if (oldType == newType) {
                    // 같은 버튼 또 누름 -> 취소
                    interactionRepository.delete(interaction);
                    decreaseCount(metadataId, oldType);

                    // [이벤트 발행] 취소되었으므로 newType은 null로 보냄
                    eventPublisher.publishEvent(new InteractionEvent(userId, metadataId, oldType, null));
                } else {
                    // 변경
                    interaction.changeType(newType);
                    interactionRepository.save(interaction);

                    decreaseCount(metadataId, oldType); // 기존 거 깎고
                    increaseCount(metadataId, newType); // 새 거 올림
                    // [이벤트 발행] 변경 전/후 타입 모두 보냄
                    eventPublisher.publishEvent(new InteractionEvent(userId, metadataId, oldType, newType));
                }
            } else {
                // 생성
                Interaction newInteraction = new Interaction(user, metadata, newType);
                interactionRepository.save(newInteraction);
                increaseCount(metadataId, newType);
                // [이벤트 발행] 새로 생성되었으므로 oldType은 null로 보냄
                eventPublisher.publishEvent(new InteractionEvent(userId, metadataId, null, newType));
            }
        }

    // 조회
    @Transactional(readOnly = true)
    public Optional<InteractionType> getInteractionStatus(Long userId, Long videoId) {
        return interactionRepository.findByUserIdAndVideoId(userId, videoId)
                .map(Interaction::getInteractionType);
    }

    private void increaseCount(Long videoMetadataId, InteractionType type) {
        switch (type) {
            case LIKE -> videoMetadataRepository.increaseLikeCount(videoMetadataId);
            case DISLIKE -> videoMetadataRepository.increaseDislikeCount(videoMetadataId);
            case SUPERLIKE -> videoMetadataRepository.increaseSuperLikeCount(videoMetadataId);
        }
    }

    private void decreaseCount(Long videoMetadataId, InteractionType type) {
        switch (type) {
            case LIKE -> videoMetadataRepository.decreaseLikeCount(videoMetadataId);
            case DISLIKE -> videoMetadataRepository.decreaseDislikeCount(videoMetadataId);
            case SUPERLIKE -> videoMetadataRepository.decreaseSuperLikeCount(videoMetadataId);
        }
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