package com.ott.core.modules.user.service;

import com.ott.common.persistence.entity.Tag;
import com.ott.common.persistence.entity.User;
import com.ott.common.persistence.entity.UserPreference;
import com.ott.common.persistence.enums.UserRole;
import com.ott.core.global.exception.UserNotFoundException;
import com.ott.core.modules.preference.repository.UserPreferenceRepository;
import com.ott.core.modules.tag.repository.TagRepository;
import com.ott.core.modules.user.dto.request.UpdateUserRequest;
import com.ott.core.modules.user.dto.response.UserDetailResponse;
import com.ott.core.modules.user.dto.response.UserResponse;
import com.ott.core.modules.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserService {

    private static final double DEFAULT_PREFERENCE_SCORE = 10.0;

    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final StringRedisTemplate stringRedisTemplate;

    public UserService(UserRepository userRepository,
                       TagRepository tagRepository,
                       UserPreferenceRepository userPreferenceRepository,
                       StringRedisTemplate stringRedisTemplate) {
        this.userRepository = userRepository;
        this.tagRepository = tagRepository;
        this.userPreferenceRepository = userPreferenceRepository;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Transactional(readOnly = true)
    public UserDetailResponse getUserDetail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
        return UserDetailResponse.from(user);
    }

    /**
     * 사용자 정보 수정
     * 수정 가능 항목: 닉네임, 관심 태그
     */
    @Transactional
    public UserResponse updateUser(Long userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        if (user.isDeleted()) {
            throw new IllegalStateException("삭제된 사용자는 수정할 수 없습니다.");
        }

        if (request.nickname() != null) {
            user.changeNickname(request.nickname());
        }

        if (request.preferredTagIds() != null) {
            updatePreferredTags(user, request.preferredTagIds());
        }

        return UserResponse.from(user);
    }

    @Transactional
    public void deleteUser(Long userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        if (user.getRole() == UserRole.ADMIN) {
            throw new IllegalArgumentException("관리자는 삭제할 수 없습니다.");
        }

        user.markDeleted(reason);
    }

    @Transactional
    public void deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
        user.deactivate();
    }

    // ====== Private Methods ======

    /**
     * 선호 태그 수정 (Bulk)
     * - Set 연산으로 추가/제거 대상 식별
     * - DB/Redis Bulk 처리
     */
    private void updatePreferredTags(User user, List<Long> preferredTagIds) {
        Long userId = user.getId();
        String redisKey = "user:" + userId + ":preference";

        List<UserPreference> existingPrefs = userPreferenceRepository.findWithTagByUserId(userId);

        Set<Long> newTagIdSet = Set.copyOf(preferredTagIds);
        Set<Long> existingTagIds = existingPrefs.stream()
                .map(up -> up.getTag().getId())
                .collect(Collectors.toSet());

        // 제거 대상
        List<UserPreference> toRemove = existingPrefs.stream()
                .filter(pref -> !newTagIdSet.contains(pref.getTag().getId()))
                .collect(Collectors.toList());

        if (!toRemove.isEmpty()) {
            String[] tagNamesToRemove = toRemove.stream()
                    .map(pref -> pref.getTag().getTagName())
                    .toArray(String[]::new);
            stringRedisTemplate.opsForZSet().remove(redisKey, (Object[]) tagNamesToRemove);
            userPreferenceRepository.deleteAll(toRemove);
        }

        // 추가 대상
        Set<Long> toAddIds = new HashSet<>(newTagIdSet);
        toAddIds.removeAll(existingTagIds);

        if (!toAddIds.isEmpty()) {
            List<Tag> newTags = tagRepository.findAllById(toAddIds);

            if (newTags.isEmpty()) {
                log.warn("[프로필 수정] 존재하지 않는 tagId 요청 - userId: {}, requestedIds: {}", userId, toAddIds);
            } else {
                if (newTags.size() != toAddIds.size()) {
                    Set<Long> foundIds = newTags.stream().map(Tag::getId).collect(Collectors.toSet());
                    Set<Long> notFoundIds = new HashSet<>(toAddIds);
                    notFoundIds.removeAll(foundIds);
                    log.warn("[프로필 수정] 일부 tagId 미존재 - userId: {}, notFoundIds: {}", userId, notFoundIds);
                }

                Set<ZSetOperations.TypedTuple<String>> tuples = newTags.stream()
                        .map(tag -> (ZSetOperations.TypedTuple<String>)
                                new DefaultTypedTuple<>(tag.getTagName(), DEFAULT_PREFERENCE_SCORE))
                        .collect(Collectors.toSet());
                stringRedisTemplate.opsForZSet().add(redisKey, tuples);

                List<UserPreference> newPrefs = newTags.stream()
                        .map(tag -> new UserPreference(user, tag, DEFAULT_PREFERENCE_SCORE))
                        .collect(Collectors.toList());
                userPreferenceRepository.saveAll(newPrefs);
            }
        }

        log.info("[프로필 수정] 선호 태그 업데이트 - userId: {}, 제거: {}, 추가: {}",
                userId, toRemove.size(), toAddIds.size());
    }
}