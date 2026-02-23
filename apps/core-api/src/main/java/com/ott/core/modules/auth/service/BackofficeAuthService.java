package com.ott.core.modules.auth.service;

import com.ott.common.error.BusinessException;
import com.ott.common.error.ErrorCode;
import com.ott.common.persistence.entity.User;
import com.ott.common.persistence.enums.BanStatus;
import com.ott.common.persistence.enums.UserRole;
import com.ott.core.global.security.jwt.JwtTokenProvider;
import com.ott.core.modules.auth.dto.BackofficeLoginRequest;
import com.ott.core.modules.auth.dto.BackofficeLoginResponse;
import com.ott.core.modules.auth.dto.BackofficeSignupRequest;
import com.ott.core.modules.user.dto.response.UserResponse;
import com.ott.core.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 백오피스 인증 서비스 (업로더/관리자용)
 *
 * 역할:
 * 1. 이메일 + 비밀번호 로그인 (업로더, 관리자 공용)
 * 2. 업로더 회원가입 (자체 가입)
 * 3. 업로더 계정 탈퇴 (Soft Delete)
 *
 * 관리자 계정은 DB에 사전 생성되어 있다고 가정합니다. (회원가입 API 없음)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackofficeAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 백오피스 로그인 (이메일 + 비밀번호)
     * UPLOADER 또는 ADMIN만 로그인 가능
     */
    @Transactional
    public BackofficeLoginResponse login(BackofficeLoginRequest request) {
        User user = userRepository.findByEmailAndNotDeleted(request.email())
                .orElseThrow(() -> {
                    log.warn("[백오피스 로그인] 존재하지 않는 이메일: {}", request.email());
                    return new BusinessException(ErrorCode.UNAUTHORIZED);
                });

        // VIEWER는 백오피스 로그인 불가
        if (user.getRole() == UserRole.VIEWER) {
            log.warn("[백오피스 로그인] VIEWER 계정으로 백오피스 로그인 시도 - userId: {}", user.getId());
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 비밀번호 검증
        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("[백오피스 로그인] 비밀번호 불일치 - userId: {}", user.getId());
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 로그인 가능 상태 확인
        validateLoginStatus(user);

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        log.info("[백오피스 로그인] 로그인 성공 - userId: {}, role: {}", user.getId(), user.getRole());
        return BackofficeLoginResponse.of(user, accessToken, refreshToken);
    }

    /**
     * 업로더 회원가입 (자체 가입)
     */
    @Transactional
    public UserResponse signupUploader(BackofficeSignupRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new BusinessException(ErrorCode.USER_DUPLICATE_EMAIL);
        }

        String passwordHash = passwordEncoder.encode(request.password());
        User user = new User(request.email(), request.nickname(), passwordHash, UserRole.UPLOADER);

        userRepository.save(user);

        log.info("[백오피스 회원가입] 업로더 계정 생성 - userId: {}, email: {}", user.getId(), user.getEmail());
        return UserResponse.from(user);
    }

    /**
     * 업로더 계정 탈퇴 (Soft Delete)
     */
    @Transactional
    public void deleteUploaderAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getRole() != UserRole.UPLOADER) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        if (user.isDeleted()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        user.markDeleted("업로더 본인 탈퇴");
        log.info("[백오피스] 업로더 계정 탈퇴 처리 - userId: {}", userId);
    }

    /**
     * 이메일 중복 체크
     */
    @Transactional(readOnly = true)
    public boolean isEmailExists(String email) {
        return userRepository.existsByEmailAndNotDeleted(email);
    }

    // ====== Private Methods ======

    /**
     * 로그인 가능 상태 검증
     *
     * AuthService.canLogin()과 동일한 로직을 사용하여 일관성을 유지합니다.
     * - DEACTIVATED: 자동 활성화 후 로그인 허용
     * - SUSPENDED (정지 기간 만료): canLogin()에서 true 반환하므로 통과
     * - SUSPENDED (정지 기간 유효): 로그인 차단
     * - DELETED: 로그인 차단
     */
    private void validateLoginStatus(User user) {
        if (user.isDeleted()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 사용자 본인이 비활성화한 경우, 로그인 시 자동 활성화
        if (user.getBanned() == BanStatus.DEACTIVATED) {
            user.activate();
            log.info("[백오피스 로그인] 비활성화 계정 자동 활성화 - userId: {}", user.getId());
        }

        // canLogin()은 정지 기간 만료 시 자동 활성화 처리 로직을 포함
        if (!user.canLogin()) {
            log.warn("[백오피스 로그인] 정지된 계정 로그인 시도 - userId: {}, status: {}", user.getId(), user.getBanned());
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}