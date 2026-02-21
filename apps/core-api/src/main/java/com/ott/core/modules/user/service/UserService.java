package com.ott.core.modules.user.service;

import com.ott.common.persistence.entity.User;
import com.ott.common.persistence.enums.UserRole;
import com.ott.core.global.exception.UserNotFoundException;
import com.ott.core.modules.user.dto.request.CreateUserRequest;
import com.ott.core.modules.user.dto.request.UpdateUserRequest;
import com.ott.core.modules.user.dto.response.UserResponse;
import com.ott.core.modules.user.dto.response.UserDetailResponse;
import com.ott.core.modules.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
}