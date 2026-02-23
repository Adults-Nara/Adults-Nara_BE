package com.ott.core.modules.user.service;

import com.ott.common.persistence.entity.Tag;
import com.ott.common.persistence.entity.User;
import com.ott.common.persistence.entity.UserPreference;
import com.ott.common.persistence.enums.UserRole;
import com.ott.core.global.exception.UserNotFoundException;
import com.ott.core.modules.preference.repository.UserPreferenceRepository;
import com.ott.core.modules.tag.repository.TagRepository;
import com.ott.core.modules.user.dto.request.CreateUserRequest;
import com.ott.core.modules.user.dto.request.UpdateUserRequest;
import com.ott.core.modules.user.dto.response.UserResponse;
import com.ott.core.modules.user.dto.response.UserDetailResponse;
import com.ott.core.modules.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TagRepository tagRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final StringRedisTemplate stringRedisTemplate;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       TagRepository tagRepository,
                       UserPreferenceRepository userPreferenceRepository,
                       StringRedisTemplate stringRedisTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tagRepository = tagRepository;
        this.userPreferenceRepository = userPreferenceRepository;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmailAndNotDeleted(request.email())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        String passwordHash = passwordEncoder.encode(request.password());
        User user = new User(request.email(), request.nickname(), passwordHash, UserRole.VIEWER);

        if (request.profileImageUrl() != null && !request.profileImageUrl().isBlank()) {
            user.changeProfileImage(request.profileImageUrl());
        }

        userRepository.save(user);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse createUploader(CreateUserRequest request, String inviteCode) {
        if (!"UPLOADER_INVITE_2026".equals(inviteCode)) {
            throw new IllegalArgumentException("유효하지 않은 초대 코드입니다.");
        }

        if (userRepository.existsByEmailAndNotDeleted(request.email())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        String passwordHash = passwordEncoder.encode(request.password());
        User user = new User(request.email(), request.nickname(), passwordHash, UserRole.UPLOADER);

        if (request.profileImageUrl() != null) {
            user.changeProfileImage(request.profileImageUrl());
        }

        userRepository.save(user);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse createAdmin(CreateUserRequest request, String adminSecretKey) {
        if (!"ADMIN_SECRET_KEY_2026".equals(adminSecretKey)) {
            throw new IllegalArgumentException("관리자 생성 권한이 없습니다.");
        }

        if (userRepository.existsByEmailAndNotDeleted(request.email())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        String passwordHash = passwordEncoder.encode(request.password());
        User user = new User(request.email(), request.nickname(), passwordHash, UserRole.ADMIN);

        userRepository.save(user);
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public UserDetailResponse getUserDetail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        return UserDetailResponse.from(user);
    }

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

        if (request.password() != null) {
            String newPasswordHash = passwordEncoder.encode(request.password());
            user.setPasswordHash(newPasswordHash);
        }

        if (request.profileImageUrl() != null) {
            user.changeProfileImage(request.profileImageUrl());
        }

        // 선호 태그 수정
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
     * 선호 태그 수정 (Bulk Operation)
     *
     * DB/Redis에 대한 호출을 최소화하기 위해 Set 연산으로 추가/제거 대상을 미리 계산하고
     * Bulk 단위로 처리합니다.
     *
     * 1. Set 연산으로 제거할 태그(toRemove)와 추가할 태그(toAdd) 식별
     * 2. DB: deleteAll / saveAll로 일괄 처리
     * 3. Redis: ZREM / ZADD로 일괄 처리
     */
    private void updatePreferredTags(User user, List<Long> preferredTagIds) {
        Long userId = user.getId();
        String redisKey = "user:" + userId + ":preference";

        // 기존 선호 태그 목록 조회
        List<UserPreference> existingPrefs = userPreferenceRepository.findWithTagByUserId(userId);

        Set<Long> newTagIdSet = Set.copyOf(preferredTagIds);
        Set<Long> existingTagIds = existingPrefs.stream()
                .map(up -> up.getTag().getId())
                .collect(Collectors.toSet());

        // === 1. 제거 대상 식별 및 Bulk 삭제 ===
        List<UserPreference> toRemove = existingPrefs.stream()
                .filter(pref -> !newTagIdSet.contains(pref.getTag().getId()))
                .collect(Collectors.toList());

        if (!toRemove.isEmpty()) {
            // Redis: ZREM 한 번에 여러 멤버 삭제
            String[] tagNamesToRemove = toRemove.stream()
                    .map(pref -> pref.getTag().getTagName())
                    .toArray(String[]::new);
            stringRedisTemplate.opsForZSet().remove(redisKey, (Object[]) tagNamesToRemove);

            // DB: 일괄 삭제
            userPreferenceRepository.deleteAll(toRemove);
        }

        // === 2. 추가 대상 식별 및 Bulk 추가 ===
        Set<Long> toAddIds = new HashSet<>(newTagIdSet);
        toAddIds.removeAll(existingTagIds);

        if (!toAddIds.isEmpty()) {
            List<Tag> newTags = tagRepository.findAllById(toAddIds);
            double defaultPreferenceScore = 10.0;

            // Redis: ZADD 한 번에 여러 멤버+점수 추가
            Set<ZSetOperations.TypedTuple<String>> tuples = newTags.stream()
                    .map(tag -> (ZSetOperations.TypedTuple<String>)
                            new DefaultTypedTuple<>(tag.getTagName(), defaultPreferenceScore))
                    .collect(Collectors.toSet());
            stringRedisTemplate.opsForZSet().add(redisKey, tuples);

            // DB: 일괄 추가
            List<UserPreference> newPrefs = newTags.stream()
                    .map(tag -> new UserPreference(user, tag, defaultPreferenceScore))
                    .collect(Collectors.toList());
            userPreferenceRepository.saveAll(newPrefs);
        }

        log.info("[프로필 수정] 선호 태그 업데이트 - userId: {}, 제거: {}, 추가: {}", userId, toRemove.size(), toAddIds.size());
    }
}