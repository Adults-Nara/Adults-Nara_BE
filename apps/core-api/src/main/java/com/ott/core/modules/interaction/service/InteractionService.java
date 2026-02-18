package com.ott.core.modules.interaction.service;

import com.ott.common.persistence.entity.*;
import com.ott.common.persistence.enums.InteractionType;
import com.ott.core.modules.interaction.repository.InteractionRepository;
import com.ott.core.modules.user.repository.UserRepository;
import com.ott.core.modules.video.repository.VideoMetadataRepository;
import lombok.RequiredArgsConstructor;
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

    public void interact(Long userId, Long videoId, InteractionType newType) {
        User user = findUser(userId);
        VideoMetadata video = findVideo(videoId);

        Optional<Interaction> existingInteraction = interactionRepository.findByUserAndVideoMetadata(user, video);

            if (existingInteraction.isPresent()) {
                Interaction interaction = existingInteraction.get();
                InteractionType oldType = interaction.getInteractionType();

                if (oldType == newType) {
                    // 같은 버튼 또 누름 -> 취소
                    interactionRepository.delete(interaction);
                    decreaseCount(video.getId(), oldType);
                } else {
                    // 변경
                    interaction.changeType(newType);
                    interactionRepository.save(interaction);

                    decreaseCount(video.getId(), oldType); // 기존 거 깎고
                    increaseCount(video.getId(), newType); // 새 거 올림
                }
            } else {
                // 생성
                Interaction newInteraction = new Interaction(user, video, newType);
                interactionRepository.save(newInteraction);
                increaseCount(video.getId(), newType);
            }
        }


    // 조회
    @Transactional(readOnly = true)
    public Optional<InteractionType> getInteractionStatus(Long userId, Long videoId) {
        return interactionRepository.findByUserIdAndVideoId(userId, videoId)
                .map(Interaction::getInteractionType);
    }

    private void increaseCount(Long videoId, InteractionType type) {
        switch (type) {
            case LIKE -> videoMetadataRepository.increaseLikeCount(videoId);
            case DISLIKE -> videoMetadataRepository.increaseDislikeCount(videoId);
            case SUPERLIKE -> videoMetadataRepository.increaseSuperLikeCount(videoId);
        }
    }

    private void decreaseCount(Long videoId, InteractionType type) {
        switch (type) {
            case LIKE -> videoMetadataRepository.decreaseLikeCount(videoId);
            case DISLIKE -> videoMetadataRepository.decreaseDislikeCount(videoId);
            case SUPERLIKE -> videoMetadataRepository.decreaseSuperLikeCount(videoId);
        }
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다. ID: " + userId));
    }

    private VideoMetadata findVideo(Long videoId) {
        return videoMetadataRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 비디오입니다. ID: " + videoId));
    }
}