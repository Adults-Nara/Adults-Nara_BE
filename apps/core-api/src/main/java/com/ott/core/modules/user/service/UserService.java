package com.ott.core.modules.user.service;

import com.ott.common.persistence.entity.User;
import com.ott.common.persistence.enums.BanStatus;
import com.ott.common.persistence.enums.UserRole;
import com.ott.common.util.IdGenerator;
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

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 사용자 생성 (회원가입)
     */
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        // 이메일 중복 확인
        if (userRepository.existsByEmailAndNotDeleted(request.email())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 비밀번호 암호화
        String passwordHash = passwordEncoder.encode(request.password());

        // User 생성
        Long userId = IdGenerator.generate();
        User user = new User(userId, request.email(), passwordHash, request.nickname(), request.role());

        // 프로필 이미지 설정 (있는 경우)
        if (request.profileImageUrl() != null && !request.profileImageUrl().isBlank()) {
            user.updateProfileImage(request.profileImageUrl());
        }

        userRepository.save(user);

        return UserResponse.from(user);
    }

    /**
     * 사용자 목록 조회 (삭제되지 않은 사용자만)
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAllNotDeleted(pageable)
                .map(UserResponse::from);
    }

    /**
     * 역할별 사용자 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> getUsersByRole(UserRole role, Pageable pageable) {
        return userRepository.findByRoleAndNotDeleted(role, pageable)
                .map(UserResponse::from);
    }

    /**
     * 사용자 상세 조회
     */
    @Transactional(readOnly = true)
    public UserDetailResponse getUserDetail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return UserDetailResponse.from(user);
    }

    /**
     * 사용자 정보 수정
     */
    @Transactional
    public UserResponse updateUser(Long userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (user.isDeleted()) {
            throw new IllegalStateException("삭제된 사용자는 수정할 수 없습니다.");
        }

        if (request.nickname() != null) {
            user.updateNickname(request.nickname());
        }

        if (request.password() != null) {
            String newPasswordHash = passwordEncoder.encode(request.password());
            user.updatePassword(newPasswordHash);
        }

        if (request.profileImageUrl() != null) {
            user.updateProfileImage(request.profileImageUrl());
        }

        return UserResponse.from(user);
    }

    /**
     * 사용자 정지 (관리자 기능)
     */
    @Transactional
    public void banUser(Long userId, BanUserRequest request, Long adminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (user.getRole() == UserRole.ADMIN) {
            throw new IllegalArgumentException("관리자는 정지할 수 없습니다.");
        }

        switch (request.banStatus()) {
            case SUSPENDED_7 -> user.suspend7Days(request.reason(), adminId);
            case SUSPENDED_15 -> user.suspend15Days(request.reason(), adminId);
            case SUSPENDED_30 -> user.suspend30Days(request.reason(), adminId);
            case PERMANENTLY_BANNED -> user.banPermanently(request.reason(), adminId);
            default -> throw new IllegalArgumentException("유효하지 않은 정지 상태입니다.");
        }
    }

    /**
     * 사용자 정지 해제 (관리자 기능)
     */
    @Transactional
    public void unbanUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        user.activate();
    }

    /**
     * 사용자 삭제 (소프트 삭제)
     */
    @Transactional
    public void deleteUser(Long userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (user.getRole() == UserRole.ADMIN) {
            throw new IllegalArgumentException("관리자는 삭제할 수 없습니다.");
        }

        user.delete(reason);
    }

    /**
     * 사용자 비활성화 (본인 요청)
     */
    @Transactional
    public void deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        user.deactivate();
    }
}