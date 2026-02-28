package com.ott.core.modules.auth.service;

import com.ott.common.error.BusinessException;
import com.ott.common.error.ErrorCode;
import com.ott.common.persistence.entity.User;
import com.ott.common.persistence.entity.UserPointBalance;
import com.ott.common.persistence.enums.BanStatus;
import com.ott.core.global.security.jwt.JwtTokenProvider;
import com.ott.core.modules.auth.dto.KakaoTokenResponse;
import com.ott.core.modules.auth.dto.KakaoUserInfoResponse;
import com.ott.core.modules.auth.dto.LoginResponse;
import com.ott.core.modules.auth.dto.TokenRefreshResponse;
import com.ott.core.modules.point.repository.PointRepository;
import com.ott.core.modules.user.dto.response.UserDetailResponse;
import com.ott.core.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String REFRESH_TOKEN_PREFIX = "refresh:token:";
    // 90일 (초 단위)
    private static final long REFRESH_TOKEN_TTL_SECONDS = 90L * 24 * 60 * 60;

    private final KakaoOAuthService kakaoOAuthService;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PointRepository pointRepository;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 카카오 로그인 처리
     */
    @Transactional
    public LoginResponse kakaoLogin(String authorizationCode, String state) {
        // 1. 카카오 액세스 토큰 교환
        KakaoTokenResponse tokenResponse = kakaoOAuthService.getAccessToken(authorizationCode);

        // 2. 카카오 사용자 정보 조회
        KakaoUserInfoResponse kakaoUser = kakaoOAuthService.getUserInfo(tokenResponse.accessToken());

        // 3. Find or Create
        String oauthProvider = "KAKAO";
        String oauthId = kakaoUser.oauthId();

        Optional<User> existingUser = userRepository.findByOauthProviderAndOauthId(oauthProvider, oauthId);

        boolean isNewUser;
        User user;

        if (existingUser.isPresent()) {
            user = existingUser.get();
            isNewUser = false;
            log.info("[카카오 로그인] 기존 사용자 로그인 - userId: {}, email: {}", user.getId(), user.getEmail());
            updateProfileIfChanged(user, kakaoUser);
        } else {
            Optional<User> emailUser = Optional.empty();
            if (kakaoUser.isEmailVerified()) {
                emailUser = userRepository.findByEmailAndNotDeleted(kakaoUser.email());
            } else {
                log.warn("[카카오 로그인] 미검증 이메일 - 이메일 기반 계정 연동 건너뜀. kakaoId: {}, email: {}",
                        kakaoUser.oauthId(), kakaoUser.email());
            }

            if (emailUser.isPresent()) {
                user = emailUser.get();
                user.setOAuth(oauthProvider, oauthId);
                isNewUser = false;
                log.info("[카카오 로그인] 기존 이메일 사용자에 카카오 연동 - userId: {}", user.getId());
            } else {
                user = new User(
                        kakaoUser.email(),
                        kakaoUser.nickname(),
                        oauthProvider,
                        oauthId
                );

                if (kakaoUser.profileImageUrl() != null) {
                    user.changeProfileImage(kakaoUser.profileImageUrl());
                }

                userRepository.save(user);
                pointRepository.save(UserPointBalance.builder()
                        .userId(user.getId())
                        .currentBalance(0)
                        .lastUpdatedAt(OffsetDateTime.now())
                        .build());

                isNewUser = true;
                log.info("[카카오 로그인] 신규 사용자 자동 회원가입 - userId: {}, email: {}", user.getId(), user.getEmail());
            }
        }

        // 4. 로그인 가능 상태 확인
        validateLoginStatus(user);

        // 5. JWT 토큰 발급
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        // 6. RefreshToken → Redis 저장 (90일 TTL)
        String redisKey = REFRESH_TOKEN_PREFIX + user.getId();
        stringRedisTemplate.opsForValue().set(redisKey, refreshToken, REFRESH_TOKEN_TTL_SECONDS, TimeUnit.SECONDS);
        log.info("[카카오 로그인] RefreshToken Redis 저장 완료 - userId: {}", user.getId());

        return LoginResponse.of(user, accessToken, refreshToken, isNewUser);
    }

    /**
     * 현재 로그인한 사용자 정보 조회
     */
    @Transactional(readOnly = true)
    public UserDetailResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.isDeleted()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        return UserDetailResponse.from(user);
    }

    /**
     * Refresh Token으로 Access Token 재발급
     */
    public TokenRefreshResponse refreshAccessToken(String refreshToken) {
        // 1. 토큰 서명/만료 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 2. RefreshToken 타입 확인
        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            log.warn("[토큰 갱신] Access Token으로 갱신 시도 차단");
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Long userId = jwtTokenProvider.getUserId(refreshToken);

        // 3. Redis에 저장된 토큰과 비교 (탈취/로그아웃 여부 확인)
        String redisKey = REFRESH_TOKEN_PREFIX + userId;
        String storedToken = stringRedisTemplate.opsForValue().get(redisKey);

        if (storedToken == null) {
            log.warn("[토큰 갱신] Redis에 RefreshToken 없음 - 로그아웃된 사용자 또는 만료 - userId: {}", userId);
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        if (!storedToken.equals(refreshToken)) {
            log.warn("[토큰 갱신] Redis 저장 토큰 불일치 - 탈취 의심 - userId: {}", userId);
            // 탈취 의심 시 Redis 토큰 즉시 삭제 (강제 로그아웃)
            stringRedisTemplate.delete(redisKey);
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 4. 유저 상태 검증
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        validateLoginStatus(user);

        // 5. 새 AccessToken 발급
        String newAccessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole().name());

        log.info("[토큰 갱신] 새 AccessToken 발급 완료 - userId: {}", userId);
        return new TokenRefreshResponse(newAccessToken);
    }

    /**
     * 로그아웃 - Redis에서 RefreshToken 삭제
     */
    public void logout(Long userId) {
        String redisKey = REFRESH_TOKEN_PREFIX + userId;
        Boolean deleted = stringRedisTemplate.delete(redisKey);

        if (Boolean.TRUE.equals(deleted)) {
            log.info("[로그아웃] RefreshToken Redis 삭제 완료 - userId: {}", userId);
        } else {
            log.warn("[로그아웃] 이미 로그아웃된 사용자 - userId: {}", userId);
        }
    }

    // ====== Private Methods ======

    private void validateLoginStatus(User user) {
        if (!user.canLogin()) {
            if (user.isDeleted()) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND);
            }
            if (user.getBanned() == BanStatus.DEACTIVATED) {
                user.activate();
                log.info("[카카오 로그인] 비활성화 계정 자동 활성화 - userId: {}", user.getId());
                return;
            }
            if (user.isSuspended()) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
        }
    }

    private void updateProfileIfChanged(User user, KakaoUserInfoResponse kakaoUser) {
        boolean changed = false;

        if (kakaoUser.nickname() != null && !kakaoUser.nickname().equals(user.getNickname())) {
            user.changeNickname(kakaoUser.nickname());
            changed = true;
        }

        if (kakaoUser.profileImageUrl() != null && !kakaoUser.profileImageUrl().equals(user.getProfileImageUrl())) {
            user.changeProfileImage(kakaoUser.profileImageUrl());
            changed = true;
        }

        if (changed) {
            log.info("[카카오 로그인] 프로필 정보 업데이트 - userId: {}", user.getId());
        }
    }
}