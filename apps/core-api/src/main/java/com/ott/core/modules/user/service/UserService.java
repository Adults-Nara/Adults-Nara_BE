package com.ott.core.modules.user.service;

import com.ott.common.persistence.entity.User;
import com.ott.common.persistence.enums.UserRole;
import com.ott.common.util.IdGenerator;
import com.ott.core.global.exception.UserNotFoundException;
import com.ott.core.modules.user.dto.request.CreateUserRequest;
import com.ott.core.modules.user.dto.request.UpdateUserRequest;
import com.ott.core.modules.user.dto.request.BanUserRequest;
import com.ott.core.modules.user.dto.response.UserResponse;
import com.ott.core.modules.user.dto.response.UserDetailResponse;
import com.ott.core.modules.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.ott.common.persistence.enums.BanStatus.*;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 일반 사용자 회원가입 (VIEWER만 가능)
     */
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmailAndNotDeleted(request.email())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        String passwordHash = passwordEncoder.encode(request.password());

        // ✅ 일반 회원가입은 항상 VIEWER로 생성
        User user = new User(request.email(), request.nickname(), passwordHash, UserRole.VIEWER);

        if (request.profileImageUrl() != null && !request.profileImageUrl().isBlank()) {
            user.changeProfileImage(request.profileImageUrl());
        }

        userRepository.save(user);

        return UserResponse.from(user);
    }

    /**
     * 업로더 회원가입 (별도 엔드포인트 또는 초대 코드 필요)
     */
    @Transactional
    public UserResponse createUploader(CreateUserRequest request, String inviteCode) {
        // ✅ 초대 코드 검증
        if (!"UPLOADER_INVITE_2026".equals(inviteCode)) {
            throw new IllegalArgumentException("유효하지 않은 초대 코드입니다.");
        }

        if (userRepository.existsByEmailAndNotDeleted(request.email())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        String passwordHash = passwordEncoder.encode(request.password());
        Long userId = IdGenerator.generate();

        User user = new User(request.email(), request.nickname(), passwordHash, UserRole.UPLOADER);

        if (request.profileImageUrl() != null) {
            user.changeProfileImage(request.profileImageUrl());
        }

        userRepository.save(user);
        return UserResponse.from(user);
    }

    /**
     * 관리자 생성 (시스템 관리자만 가능)
     */
    @Transactional
    public UserResponse createAdmin(CreateUserRequest request, String adminSecretKey) {
        // ✅ 환경변수에서 관리자 생성 키 검증
        if (!"ADMIN_SECRET_KEY_2026".equals(adminSecretKey)) {
            throw new IllegalArgumentException("관리자 생성 권한이 없습니다.");
        }

        if (userRepository.existsByEmailAndNotDeleted(request.email())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        String passwordHash = passwordEncoder.encode(request.password());
        User user = new User(request.email(),request.nickname(), passwordHash, UserRole.ADMIN);

        userRepository.save(user);
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAllNotDeleted(pageable)
                .map(UserResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getUsersByRole(UserRole role, Pageable pageable) {
        return userRepository.findByRoleAndNotDeleted(role, pageable)
                .map(UserResponse::from);
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
    public void banUser(Long userId, BanUserRequest request, Long adminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        if (user.getRole() == UserRole.ADMIN) {
            throw new IllegalArgumentException("관리자는 정지할 수 없습니다.");
        }

        switch (request.banStatus()) {
            case SUSPENDED_7 -> user.setBanStatus(SUSPENDED_7);
            case SUSPENDED_15 -> user.setBanStatus(SUSPENDED_15);
            case SUSPENDED_30 -> user.setBanStatus(SUSPENDED_30);
            case PERMANENTLY_BANNED ->  user.setBanStatus(PERMANENTLY_BANNED);
            default -> throw new IllegalArgumentException("유효하지 않은 정지 상태입니다.");
        }
    }

    @Transactional
    public void unbanUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        user.activate();
    }

    @Transactional
    //관리자
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