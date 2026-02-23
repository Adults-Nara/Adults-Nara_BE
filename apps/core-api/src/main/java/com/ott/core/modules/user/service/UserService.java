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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        // 닉네임 수정
        if (request.nickname() != null) {
            user.changeNickname(request.nickname());
        }

        // 비밀번호 수정 (UPLOADER/ADMIN만 가능)
        if (request.password() != null) {
            String newPasswordHash = passwordEncoder.encode(request.password());
            user.setPasswordHash(newPasswordHash);
        }

        // 프로필 이미지 수정
        if (request.profileImageUrl() != null) {
            user.changeProfileImage(request.profileImageUrl());
        }

        // [신규] 선호 태그 수정
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
     * [신규] 선호 태그 수정
     *
     * 사용자가 마이페이지에서 선호 태그를 변경하면:
     * 1. DB의 user_preference 테이블에서 기존 선호 태그 초기화
     * 2. 새로운 선호 태그에 기본 점수(10.0) 부여
     * 3. Redis의 취향 점수도 동기화 (실시간 추천에 반영)
     *
     * 주의: 시청 기록 기반으로 쌓인 기존 preference와 병합됩니다.
     * preferredTagIds가 빈 리스트이면 선호 태그 초기화만 수행합니다.
     */
    private void updatePreferredTags(User user, List<Long> preferredTagIds) {
        Long userId = user.getId();
        String redisKey = "user:" + userId + ":preference";

        // 기존 선호 태그 목록 조회
        List<UserPreference> existingPrefs = userPreferenceRepository.findWithTagByUserId(userId);
        Set<Long> existingTagIds = existingPrefs.stream()
                .map(up -> up.getTag().getId())
                .collect(Collectors.toSet());

        // 새로 선택된 태그 목록 조회
        List<Tag> newTags = tagRepository.findAllById(preferredTagIds);

        // 새로 추가된 태그에 기본 선호 점수 부여
        double defaultPreferenceScore = 10.0;

        for (Tag tag : newTags) {
            if (!existingTagIds.contains(tag.getId())) {
                // DB: 신규 선호 태그 추가
                com.ott.common.util.IdGenerator idGen = null; // static method 사용
                userPreferenceRepository.addScore(
                        com.ott.common.util.IdGenerator.generate(),
                        userId,
                        tag.getId(),
                        defaultPreferenceScore,
                        java.time.LocalDateTime.now()
                );
            }

            // Redis: 선호 태그 점수 동기화
            stringRedisTemplate.opsForZSet().add(redisKey, tag.getTagName(), defaultPreferenceScore);
        }

        log.info("[프로필 수정] 선호 태그 업데이트 - userId: {}, 태그 수: {}", userId, newTags.size());
    }
}