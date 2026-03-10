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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * 백오피스 인증 서비스 (업로더/관리자용)
 *
 * 역할:
 * 1. 이메일 + 비밀번호 로그인 (업로더, 관리자 공용)
 * 2. 업로더 회원가입 (자체 가입)
 * 3. 업로더 계정 탈퇴 (Soft Delete)
 * 4. Access Token 재발급 (Refresh Token 기반)
 * 5. 로그아웃 (Redis Refresh Token 삭제)
 *
 * 토큰 정책 (카카오 로그인과 동일):
 * - Access Token  → Response body
 * - Refresh Token → HttpOnly 쿠키 + Redis(90일 TTL)
 * - 로그아웃 시 Redis에서 Refresh Token 즉시 삭제
 * - 탈취 의심 시 Redis 토큰 삭제 → 강제 로그아웃
 *
 * 관리자 계정은 DB에 사전 생성되어 있다고 가정합니다. (회원가입 API 없음)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackofficeAuthService {

    // 카카오(VIEWER) 세션과 키 충돌 방지를 위해 별도 prefix 사용
    // 카카오: "refresh:token:{userId}" / 백오피스: "backoffice:refresh:token:{userId}"
    private static final String REFRESH_TOKEN_PREFIX = "backoffice:refresh:token:";
    private static final long REFRESH_TOKEN_TTL_SECONDS = 90L * 24 * 60 * 60; // 90일

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 백오피스 로그인 (이메일 + 비밀번호)
     * UPLOADER 또는 ADMIN만 로그인 가능
     *
     * 카카오 로그인과 동일한 토큰 정책 적용:
     * - Refresh Token → Redis 저장 (90일 TTL)
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

        // JWT 토큰 발급
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        // Refresh Token → Redis 저장 (90일 TTL)
        saveRefreshToken(user.getId(), refreshToken);

        log.info("[백오피스 로그인] 로그인 성공 - userId: {}, role: {}", user.getId(), user.getRole());
        return BackofficeLoginResponse.of(user, accessToken, refreshToken);
    }

    /**
     * Access Token 재발급 + Refresh Token Rotation (RTR)
     *
     * RTR 정책:
     * - 갱신 시마다 새 Refresh Token 발급 → Redis 갱신 → 새 쿠키 세팅
     * - 기존 Refresh Token 즉시 무효화
     * - 이전 토큰으로 재시도 시 탈취 의심 → 강제 로그아웃
     *
     * 반환: accessToken + 새 refreshToken (컨트롤러에서 쿠키 재발급)
     */
    public BackofficeLoginResponse refreshAccessToken(String refreshToken) {
        // 1. 토큰 서명/만료 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 2. Refresh Token 타입 확인 (Access Token으로 갱신 시도 차단)
        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            log.warn("[백오피스 토큰 갱신] Access Token으로 갱신 시도 차단");
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Long userId = jwtTokenProvider.getUserId(refreshToken);

        // 3. Redis에 저장된 토큰과 비교 (탈취/로그아웃 여부 확인)
        String redisKey = REFRESH_TOKEN_PREFIX + userId;
        String storedToken = stringRedisTemplate.opsForValue().get(redisKey);

        if (storedToken == null) {
            log.warn("[백오피스 토큰 갱신] Redis에 RefreshToken 없음 - 로그아웃된 사용자 또는 만료 - userId: {}", userId);
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        if (!storedToken.equals(refreshToken)) {
            log.warn("[백오피스 토큰 갱신] Redis 저장 토큰 불일치 - 탈취 의심 → 강제 로그아웃 - userId: {}", userId);
            stringRedisTemplate.delete(redisKey);
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 4. 유저 상태 검증
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        validateLoginStatus(user);

        // 5. [RTR] 새 Access Token + 새 Refresh Token 발급
        String newAccessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole().name());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        // 6. [RTR] 기존 Refresh Token 즉시 무효화 → 새 토큰으로 교체
        saveRefreshToken(userId, newRefreshToken);

        log.info("[백오피스 토큰 갱신] AccessToken + RefreshToken 재발급 완료 (RTR) - userId: {}", userId);
        return BackofficeLoginResponse.of(user, newAccessToken, newRefreshToken);
    }

    /**
     * 로그아웃 - Redis에서 Refresh Token 삭제
     *
     * 카카오 로그인의 AuthService.logout()과 동일한 정책
     */
    public void logout(Long userId) {
        String redisKey = REFRESH_TOKEN_PREFIX + userId;
        Boolean deleted = stringRedisTemplate.delete(redisKey);

        if (Boolean.TRUE.equals(deleted)) {
            log.info("[백오피스 로그아웃] RefreshToken Redis 삭제 완료 - userId: {}", userId);
        } else {
            log.warn("[백오피스 로그아웃] 이미 로그아웃된 사용자 - userId: {}", userId);
        }
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
     * 탈퇴 시 Redis Refresh Token도 함께 삭제
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

        // 탈퇴 시 Redis Refresh Token 삭제 (강제 로그아웃)
        logout(userId);

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
     * Refresh Token Redis 저장 (90일 TTL)
     */
    private void saveRefreshToken(Long userId, String refreshToken) {
        String redisKey = REFRESH_TOKEN_PREFIX + userId;
        stringRedisTemplate.opsForValue().set(redisKey, refreshToken, REFRESH_TOKEN_TTL_SECONDS, TimeUnit.SECONDS);
        log.info("[백오피스 로그인] RefreshToken Redis 저장 완료 - userId: {}", userId);
    }

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