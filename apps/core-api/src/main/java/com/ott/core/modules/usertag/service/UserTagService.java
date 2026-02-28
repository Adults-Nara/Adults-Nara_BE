package com.ott.core.modules.usertag.service;

import com.ott.common.error.BusinessException;
import com.ott.common.error.ErrorCode;
import com.ott.common.persistence.entity.Tag;
import com.ott.common.persistence.entity.User;
import com.ott.common.persistence.entity.UserTag;
import com.ott.core.modules.tag.repository.TagRepository;
import com.ott.core.modules.user.repository.UserRepository;
import com.ott.core.modules.usertag.repository.UserTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserTagService {

    private final UserTagRepository userTagRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;

    @Transactional
    public void saveOnboardingTags(Long userId, List<Long> tagIds) {
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<Tag> tagList = tagRepository.findAllById(tagIds);
        List<UserTag> userTagList = tagList.stream()
                .map(tag -> UserTag.builder()
                        .user(user)
                        .tag(tag)
                        .build()).toList();

        userTagRepository.saveAll(userTagList);
    }

    @Transactional
    public void updateUserTags(Long userId, List<Long> newTagIds) {
        List<Long> currentTagIds = userTagRepository.findTagIdsByUserId(userId);

        List<Long> toDelete = currentTagIds.stream()
                .filter(id -> !newTagIds.contains(id))
                .toList();

        List<Long> toAdd = newTagIds.stream()
                .filter(id -> !currentTagIds.contains(id))
                .toList();

        if (!toDelete.isEmpty()) {
            userTagRepository.deleteByUserIdAndTagIdIn(userId, toDelete);
        }

        if (!toAdd.isEmpty()) {
            User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
            List<Tag> tagList = tagRepository.findAllById(toAdd);
            List<UserTag> userTagList = tagList.stream()
                    .map(tag -> UserTag.builder()
                            .user(user)
                            .tag(tag)
                            .build())
                    .toList();
            userTagRepository.saveAll(userTagList);
        }
    }
}
