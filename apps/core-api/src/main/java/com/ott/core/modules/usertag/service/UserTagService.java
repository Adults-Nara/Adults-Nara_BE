package com.ott.core.modules.usertag.service;

import com.ott.common.error.BusinessException;
import com.ott.common.error.ErrorCode;
import com.ott.common.persistence.entity.Tag;
import com.ott.common.persistence.entity.User;
import com.ott.common.persistence.entity.UserTag;
import com.ott.core.modules.tag.repository.TagRepository;
import com.ott.core.modules.user.repository.UserRepository;
import com.ott.core.modules.usertag.dto.TagWatchStatsResponse;
import com.ott.core.modules.usertag.event.UserTagsUpdatedEvent;
import com.ott.core.modules.usertag.repository.UserTagRepository;
import com.ott.core.modules.watch.repository.WatchHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserTagService {

    private final UserTagRepository userTagRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final WatchHistoryRepository watchHistoryRepository;

    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void saveOnboardingTags(Long userId, List<Long> tagIds) {
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<Tag> tagList = tagRepository.findAllById(tagIds);

        if (tagList.size() != tagIds.size()) {
            throw new BusinessException(ErrorCode.TAG_NOT_FOUND);
        }

        List<UserTag> userTagList = tagList.stream()
                .map(tag -> UserTag.builder()
                        .user(user)
                        .tag(tag)
                        .build()).toList();

        userTagRepository.saveAll(userTagList);

        List<String> addedTagNames = tagList.stream().map(Tag::getTagName).toList();
        eventPublisher.publishEvent(new UserTagsUpdatedEvent(userId, addedTagNames, List.of()));
    }

    @Transactional
    public void updateUserTags(Long userId, List<Long> newTagIds) {
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        List<Long> currentTagIds = userTagRepository.findTagIdsByUserId(userId);

        List<Long> toDelete = currentTagIds.stream()
                .filter(id -> !newTagIds.contains(id))
                .toList();

        List<Long> toAdd = newTagIds.stream()
                .filter(id -> !currentTagIds.contains(id))
                .toList();

        List<String> removedTagNames = new ArrayList<>();
        List<String> addedTagNames = new ArrayList<>();

        if (!toDelete.isEmpty()) {
            userTagRepository.deleteByUserIdAndTagIdIn(userId, toDelete);
        }

        if (!toAdd.isEmpty()) {
            List<Tag> tagList = tagRepository.findAllById(toAdd);

            if (tagList.size() != toAdd.size()) {
                throw new BusinessException(ErrorCode.TAG_NOT_FOUND);
            }

            List<UserTag> userTagList = tagList.stream()
                    .map(tag -> UserTag.builder()
                            .user(user)
                            .tag(tag)
                            .build())
                    .toList();
            userTagRepository.saveAll(userTagList);
        }
        if (!addedTagNames.isEmpty() || !removedTagNames.isEmpty()) {
            eventPublisher.publishEvent(new UserTagsUpdatedEvent(userId, addedTagNames, removedTagNames));
        }
    }

    public List<TagWatchStatsResponse> getTagWatchStats(Long userId) {
        List<Object[]> top8TagList = watchHistoryRepository.findTop8TagWatchStatsByUserId(userId);
        return top8TagList.stream()
                .map(row -> new TagWatchStatsResponse(
                        String.valueOf(((Number) row[0]).longValue()),
                        (String) row[1],
                        row[2] != null ? ((Number) row[2]).longValue() : 0L
                )).toList();
    }
}
