package com.ott.core.modules.auth.service;

import com.ott.common.error.BusinessException;
import com.ott.common.error.ErrorCode;
import com.ott.common.persistence.entity.User;
import com.ott.common.persistence.enums.BanStatus;
import com.ott.core.global.security.jwt.JwtTokenProvider;
import com.ott.core.modules.auth.dto.KakaoTokenResponse;
import com.ott.core.modules.auth.dto.KakaoUserInfoResponse;
import com.ott.core.modules.auth.dto.LoginResponse;
import com.ott.core.modules.auth.dto.TokenRefreshResponse;
import com.ott.core.modules.user.dto.response.UserDetailResponse;
import com.ott.core.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 인증(Authentication) 서비스
 *
 * 역할:
 * 1. 카카오 OAuth 로그인 처리 (회원가입/로그인 통합)
 * 2. JWT 토큰 발급
 * 3. 토큰 갱신
 * 4. 현재 사용자 정보 조회
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final KakaoOAuthService kakaoOAuthService;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 카카오 로그인 처리
     *
     * 플로우:
     * 1. 인가코드로 카카오 액세스 토큰 교환
     * 2. 카카오 액세스 토큰으로 사용자 정보 조회
     * 3. DB에서 사용자 찾기 (oauth_provider + oauth_id)
     *    - 있으면: 기존 사용자 로그인
     *    - 없으면: 새 사용자 생성 (회원가입)
     * 4. 로그인 가능 상태 확인 (밴, 탈퇴 체크)
     * 5. JWT 토큰 발급
     *
     * @param authorizationCode 카카오 인가코드
     * @param state OAuth state 파라미터 (CSRF 검증용)
     * @return 로그인 응답 (유저정보 + JWT 토큰)
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
            // 기존 사용자
            user = existingUser.get();
            isNewUser = false;
            log.info("[카카오 로그인] 기존 사용자 로그인 - userId: {}, email: {}", user.getId(), user.getEmail());

            // 프로필 정보 업데이트 (카카오에서 변경됐을 수 있으므로)
            updateProfileIfChanged(user, kakaoUser);
        } else {
            // [Fix #1] 이메일 기반 계정 연동 시 카카오 이메일 검증 여부 확인
            // 미검증 이메일로 기존 계정을 탈취하는 것을 방지
            Optional<User> emailUser = Optional.empty();
            if (kakaoUser.isEmailVerified()) {
                emailUser = userRepository.findByEmailAndNotDeleted(kakaoUser.email());
            } else {
                log.warn("[카카오 로그인] 미검증 이메일 - 이메일 기반 계정 연동 건너뜀. kakaoId: {}, email: {}",
                        kakaoUser.oauthId(), kakaoUser.email());
            }

            if (emailUser.isPresent()) {
                // 이메일은 같지만 OAuth가 아닌 기존 사용자 → OAuth 연동
                user = emailUser.get();
                user.setOAuth(oauthProvider, oauthId);
                isNewUser = false;
                log.info("[카카오 로그인] 기존 이메일 사용자에 카카오 연동 - userId: {}", user.getId());
            } else {
                // 완전 신규 사용자 생성 (자동 회원가입)
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
                isNewUser = true;
                log.info("[카카오 로그인] 신규 사용자 자동 회원가입 - userId: {}, email: {}", user.getId(), user.getEmail());
            }
        }

        // 4. 로그인 가능 상태 확인
        validateLoginStatus(user);

        // 5. JWT 토큰 발급
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        return LoginResponse.of(user, accessToken, refreshToken, isNewUser);
    }

    /**
     * [신규] 현재 로그인한 사용자 정보 조회
     *
     * 프론트엔드 마이페이지에서 사용합니다.
     * - 비로그인: SecurityConfig에서 인증 필요이므로 이 메서드까지 도달하지 않음
     * - 로그인: JWT에서 추출한 userId로 사용자 정보 반환
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
        // [Fix #5] Refresh Token 타입 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            log.warn("[토큰 갱신] Access Token으로 갱신 시도 차단");
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Long userId = jwtTokenProvider.getUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        validateLoginStatus(user);

        String newAccessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole().name());
        return new TokenRefreshResponse(newAccessToken);
    }

    // ====== Private Methods ======

    /**
     * [Fix #6] 비활성화 계정 처리 정책 분리
     * - DEACTIVATED: 사용자 본인이 비활성화 → 로그인 시 자동 활성화
     * - SUSPENDED: 관리자가 정지 → 로그인 차단
     */
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