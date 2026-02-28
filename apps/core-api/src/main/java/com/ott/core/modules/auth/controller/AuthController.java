package com.ott.core.modules.auth.controller;

import com.ott.common.error.BusinessException;
import com.ott.common.error.ErrorCode;
import com.ott.common.response.ApiResponse;
import com.ott.core.modules.auth.dto.LoginResponse;
import com.ott.core.modules.auth.dto.TokenRefreshResponse;
import com.ott.core.modules.auth.service.AuthService;
import com.ott.core.modules.user.dto.response.UserDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "인증 API", description = "카카오 OAuth 로그인 및 토큰 관리")
public class AuthController {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final long STATE_EXPIRY_SECONDS = 300;
    private static final String STATE_NONCE_COOKIE = "oauth_state_nonce";
    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    private static final int REFRESH_TOKEN_COOKIE_MAX_AGE = 90 * 24 * 60 * 60;

    private final AuthService authService;
    private final String kakaoClientId;
    private final String kakaoRedirectUri;
    private final byte[] stateSigningKey;
    private final boolean secureCookie;
    private final boolean validateState;

    public AuthController(
            AuthService authService,
            @Value("${oauth2.kakao.client-id}") String kakaoClientId,
            @Value("${oauth2.kakao.redirect-uri}") String kakaoRedirectUri,
            @Value("${oauth2.state.secret}") String stateSecret,
            @Value("${oauth2.state.cookie-secure:true}") boolean secureCookie,
            @Value("${oauth2.state.validate:true}") boolean validateState
    ) {
        this.authService = authService;
        this.kakaoClientId = kakaoClientId;
        this.kakaoRedirectUri = kakaoRedirectUri;
        this.stateSigningKey = Base64.getDecoder().decode(stateSecret);
        this.secureCookie = secureCookie;
        this.validateState = validateState;
    }

    @Operation(
            summary = "카카오 로그인 URL 조회",
            description = "프론트엔드에서 카카오 로그인 페이지로 이동할 URL을 반환합니다."
    )
    @GetMapping("/kakao/login-url")
    public ApiResponse<String> getKakaoLoginUrl(HttpServletResponse response) {
        byte[] nonceBytes = new byte[16];
        SECURE_RANDOM.nextBytes(nonceBytes);
        String nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(nonceBytes);

        String state = generateSignedState(nonce);

        Cookie nonceCookie = new Cookie(STATE_NONCE_COOKIE, nonce);
        nonceCookie.setHttpOnly(true);
        nonceCookie.setSecure(secureCookie);
        nonceCookie.setPath("/api/v1/auth/kakao");
        nonceCookie.setMaxAge((int) STATE_EXPIRY_SECONDS);
        nonceCookie.setAttribute("SameSite", "Lax");
        response.addCookie(nonceCookie);

        String loginUrl = "https://kauth.kakao.com/oauth/authorize"
                + "?client_id=" + kakaoClientId
                + "&redirect_uri=" + URLEncoder.encode(kakaoRedirectUri, StandardCharsets.UTF_8)
                + "&response_type=code"
                + "&state=" + state;

        return ApiResponse.success(loginUrl);
    }

    @Operation(
            summary = "카카오 로그인 (인가코드 → JWT 발급)",
            description = "카카오 인가코드를 받아 사용자 인증 후 JWT 토큰을 발급합니다. " +
                    "AccessToken은 body에, RefreshToken은 HttpOnly 쿠키로 전달됩니다."
    )
    @GetMapping("/kakao/callback")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<LoginResponse> kakaoCallback(
            @Parameter(description = "카카오 인가코드", required = true)
            @RequestParam("code") String code,
            @Parameter(description = "CSRF 방지용 state 토큰")
            @RequestParam(value = "state", required = false) String state,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (validateState) {
            if (state == null || !verifySignedState(state)) {
                log.warn("[카카오 OAuth] state 검증 실패 - CSRF 공격 의심");
                throw new BusinessException(ErrorCode.UNAUTHORIZED);
            }

            String cookieNonce = extractCookieValue(request, STATE_NONCE_COOKIE);
            if (cookieNonce == null) {
                log.warn("[카카오 OAuth] nonce 쿠키 없음 - CSRF 공격 의심");
                throw new BusinessException(ErrorCode.UNAUTHORIZED);
            }

            String stateNonce = state.split("\\.")[0];
            if (!MessageDigest.isEqual(
                    stateNonce.getBytes(StandardCharsets.UTF_8),
                    cookieNonce.getBytes(StandardCharsets.UTF_8))) {
                log.warn("[카카오 OAuth] nonce 불일치 - CSRF 공격 의심");
                throw new BusinessException(ErrorCode.UNAUTHORIZED);
            }

            clearNonceCookie(response);
        } else {
            log.warn("[카카오 OAuth] state/nonce 검증 우회 중 (로컬 테스트 모드) - 운영 환경에서 절대 사용 금지");
        }

        log.info("[카카오 OAuth] 콜백 수신 - 인가코드 길이: {}", code.length());
        LoginResponse loginResponse = authService.kakaoLogin(code, state);

        ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, loginResponse.refreshToken())
                .httpOnly(true)
                .secure(secureCookie)
                .path("/api/v1/auth/token")
                .maxAge(REFRESH_TOKEN_COOKIE_MAX_AGE)
                .sameSite("Lax")
                .build();
        response.addHeader("Set-Cookie", refreshCookie.toString());

        return ApiResponse.success(loginResponse.withoutRefreshToken());
    }

    @Operation(
            summary = "현재 로그인 사용자 정보 조회",
            description = "JWT 토큰으로 인증된 현재 사용자의 정보를 반환합니다."
    )
    @GetMapping("/me")
    public ApiResponse<UserDetailResponse> getCurrentUser(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        UserDetailResponse userDetail = authService.getCurrentUser(userId);
        return ApiResponse.success(userDetail);
    }

    @Operation(
            summary = "Access Token 재발급",
            description = "쿠키의 RefreshToken으로 새로운 AccessToken을 발급받습니다."
    )
    @PostMapping("/token/refresh")
    public ApiResponse<TokenRefreshResponse> refreshToken(HttpServletRequest request) {
        String refreshToken = extractCookieValue(request, REFRESH_TOKEN_COOKIE);

        if (refreshToken == null) {
            log.warn("[토큰 갱신] refresh_token 쿠키 없음");
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        TokenRefreshResponse tokenResponse = authService.refreshAccessToken(refreshToken);
        return ApiResponse.success(tokenResponse);
    }

    @Operation(
            summary = "로그아웃",
            description = "서버의 RefreshToken을 무효화하고 쿠키를 삭제합니다. AccessToken이 필요합니다."
    )
    @PostMapping("/token/logout")
    public ApiResponse<?> logout(Authentication authentication, HttpServletResponse response) {
        Long userId = Long.parseLong(authentication.getName());

        authService.logout(userId);

        ResponseCookie expiredCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(secureCookie)
                .path("/api/v1/auth/token")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        response.addHeader("Set-Cookie", expiredCookie.toString());

        return ApiResponse.success();
    }

    // ====== Private Methods ======

    private String generateSignedState(String nonce) {
        long timestamp = System.currentTimeMillis() / 1000;
        String payload = nonce + "." + timestamp;
        String signature = hmacSign(payload);
        return payload + "." + signature;
    }

    private boolean verifySignedState(String state) {
        try {
            String[] parts = state.split("\\.");
            if (parts.length != 3) {
                return false;
            }

            String nonce = parts[0];
            String timestampStr = parts[1];
            String signature = parts[2];

            String payload = nonce + "." + timestampStr;
            String expectedSignature = hmacSign(payload);
            if (!MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8))) {
                log.warn("[카카오 OAuth] state 서명 불일치");
                return false;
            }

            long timestamp = Long.parseLong(timestampStr);
            long now = System.currentTimeMillis() / 1000;
            if (now - timestamp > STATE_EXPIRY_SECONDS) {
                log.warn("[카카오 OAuth] state 만료 - 생성: {}초 전", now - timestamp);
                return false;
            }

            return true;
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            log.warn("[카카오 OAuth] state 파싱 실패: {}", e.getMessage());
            return false;
        }
    }

    private String hmacSign(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(stateSigningKey, HMAC_ALGORITHM));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (java.security.NoSuchAlgorithmException | java.security.InvalidKeyException e) {
            throw new IllegalStateException("HMAC 서명 생성 실패 - 애플리케이션 설정을 확인하세요", e);
        }
    }

    private String extractCookieValue(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) {
            return null;
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> cookieName.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private void clearNonceCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(STATE_NONCE_COOKIE, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(secureCookie);
        cookie.setPath("/api/v1/auth/kakao");
        cookie.setMaxAge(0);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }
}