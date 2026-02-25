package com.ott.core.modules.auth.controller;

import com.ott.common.error.BusinessException;
import com.ott.common.error.ErrorCode;
import com.ott.common.response.ApiResponse;
import com.ott.core.modules.auth.dto.LoginResponse;
import com.ott.core.modules.auth.dto.TokenRefreshRequest;
import com.ott.core.modules.auth.dto.TokenRefreshResponse;
import com.ott.core.modules.auth.service.AuthService;
import com.ott.core.modules.user.dto.response.UserDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 인증 컨트롤러 (카카오 OAuth + JWT 토큰 관리)
 *
 * CSRF state 검증: HMAC 서명 + HttpOnly 쿠키 nonce 바인딩
 *
 * [공격 시나리오 방어]
 * 단순 HMAC 서명만으로는 공격자가 자신의 state를 피해자에게 전달하는 CSRF가 가능합니다.
 * 이를 방어하기 위해 state 생성 시 nonce를 HttpOnly 쿠키에도 저장하고,
 * 콜백 시 state 내 nonce와 쿠키 nonce를 비교하여 "이 브라우저가 요청한 state"인지 검증합니다.
 *
 * state 구조: {nonce}.{timestamp}.{signature}
 * 쿠키: oauth_state_nonce = {nonce} (HttpOnly, Secure, SameSite=Lax, 5분 TTL)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "인증 API", description = "카카오 OAuth 로그인 및 토큰 관리")
public class AuthController {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final long STATE_EXPIRY_SECONDS = 300; // 5분
    private static final String STATE_NONCE_COOKIE = "oauth_state_nonce";

    private final AuthService authService;
    private final String kakaoClientId;
    private final String kakaoRedirectUri;
    private final byte[] stateSigningKey;
    private final boolean secureCookie;

    public AuthController(
            AuthService authService,
            @Value("${oauth2.kakao.client-id}") String kakaoClientId,
            @Value("${oauth2.kakao.redirect-uri}") String kakaoRedirectUri,
            @Value("${oauth2.state.secret}") String stateSecret,
            @Value("${oauth2.state.cookie-secure:true}") boolean secureCookie
    ) {
        this.authService = authService;
        this.kakaoClientId = kakaoClientId;
        this.kakaoRedirectUri = kakaoRedirectUri;
        // Base64 디코딩 대신 raw bytes 사용 — 어떤 문자열이든 안전하게 동작
        this.stateSigningKey = stateSecret.getBytes(StandardCharsets.UTF_8);
        this.secureCookie = secureCookie;
    }

    /**
     * 카카오 로그인 URL 조회
     *
     * 1. nonce + timestamp로 HMAC 서명된 state 생성
     * 2. nonce를 HttpOnly 쿠키에 저장 (브라우저 바인딩)
     * 3. 카카오 인증 URL 반환
     */
    @Operation(
            summary = "카카오 로그인 URL 조회",
            description = "프론트엔드에서 카카오 로그인 페이지로 이동할 URL을 반환합니다. " +
                    "CSRF 방지를 위한 HMAC 서명 + 쿠키 바인딩 state 파라미터가 포함됩니다."
    )
    @GetMapping("/kakao/login-url")
    public ApiResponse<String> getKakaoLoginUrl(HttpServletResponse response) {
        // nonce 생성
        byte[] nonceBytes = new byte[16];
        SECURE_RANDOM.nextBytes(nonceBytes);
        String nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(nonceBytes);

        // state 토큰 생성
        String state = generateSignedState(nonce);

        // nonce를 HttpOnly 쿠키에 저장 (브라우저 바인딩)
        Cookie nonceCookie = new Cookie(STATE_NONCE_COOKIE, nonce);
        nonceCookie.setHttpOnly(true);
        nonceCookie.setSecure(secureCookie);
        nonceCookie.setPath("/api/v1/auth/kakao");
        nonceCookie.setMaxAge((int) STATE_EXPIRY_SECONDS);
        // SameSite=Lax: 카카오 리다이렉트(GET) 시 쿠키 전송됨
        nonceCookie.setAttribute("SameSite", "Lax");
        response.addCookie(nonceCookie);

        String loginUrl = "https://kauth.kakao.com/oauth/authorize"
                + "?client_id=" + kakaoClientId
                + "&redirect_uri=" + URLEncoder.encode(kakaoRedirectUri, StandardCharsets.UTF_8)
                + "&response_type=code"
                + "&state=" + state;

        return ApiResponse.success(loginUrl);
    }

    /**
     * 카카오 OAuth 콜백 처리
     *
     * 검증 순서:
     * 1. state 파라미터 존재 및 HMAC 서명/만료 검증
     * 2. 쿠키에서 nonce 추출
     * 3. state 내 nonce와 쿠키 nonce 일치 확인 (CSRF 방지 — 핵심)
     * 4. nonce 쿠키 삭제 (일회성 보장)
     */
    @Operation(
            summary = "카카오 로그인 (인가코드 → JWT 발급)",
            description = "카카오 인가코드를 받아 사용자 인증 후 JWT 토큰을 발급합니다. " +
                    "신규 사용자는 자동으로 회원가입됩니다."
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
        // 1. state 서명 + 만료 검증
        if (state == null || !verifySignedState(state)) {
            log.warn("[카카오 OAuth] state 검증 실패 - CSRF 공격 의심");
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 2. 쿠키에서 nonce 추출
        String cookieNonce = extractCookieNonce(request);
        if (cookieNonce == null) {
            log.warn("[카카오 OAuth] nonce 쿠키 없음 - CSRF 공격 의심");
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 3. state 내 nonce와 쿠키 nonce 비교 (constant-time)
        String stateNonce = state.split("\\.")[0];
        if (!MessageDigest.isEqual(
                stateNonce.getBytes(StandardCharsets.UTF_8),
                cookieNonce.getBytes(StandardCharsets.UTF_8))) {
            log.warn("[카카오 OAuth] nonce 불일치 - CSRF 공격 의심");
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 4. nonce 쿠키 즉시 삭제 (일회성)
        clearNonceCookie(response);

        log.info("[카카오 OAuth] 콜백 수신 - 인가코드 길이: {}", code.length());
        LoginResponse loginResponse = authService.kakaoLogin(code, state);
        return ApiResponse.success(loginResponse);
    }

    /**
     * 현재 로그인한 사용자 정보 조회
     */
    @Operation(
            summary = "현재 로그인 사용자 정보 조회",
            description = "JWT 토큰으로 인증된 현재 사용자의 정보를 반환합니다. " +
                    "마이페이지 렌더링 및 로그인 상태 확인에 사용합니다."
    )
    @GetMapping("/me")
    public ApiResponse<UserDetailResponse> getCurrentUser(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        UserDetailResponse userDetail = authService.getCurrentUser(userId);
        return ApiResponse.success(userDetail);
    }

    /**
     * Access Token 재발급
     */
    @Operation(
            summary = "Access Token 재발급",
            description = "Refresh Token으로 새로운 Access Token을 발급받습니다."
    )
    @PostMapping("/token/refresh")
    public ApiResponse<TokenRefreshResponse> refreshToken(
            @Valid @RequestBody TokenRefreshRequest request
    ) {
        TokenRefreshResponse tokenResponse = authService.refreshAccessToken(request.refreshToken());
        return ApiResponse.success(tokenResponse);
    }

    // ====== Private Methods ======

    /**
     * HMAC 서명 기반 state 토큰 생성
     * 형식: {nonce}.{timestamp}.{signature}
     */
    private String generateSignedState(String nonce) {
        long timestamp = System.currentTimeMillis() / 1000;
        String payload = nonce + "." + timestamp;
        String signature = hmacSign(payload);
        return payload + "." + signature;
    }

    /**
     * state 토큰 검증 (서명 + 만료 시간)
     * nonce 쿠키 검증은 호출부(kakaoCallback)에서 별도 수행
     */
    private boolean verifySignedState(String state) {
        try {
            String[] parts = state.split("\\.");
            if (parts.length != 3) {
                return false;
            }

            String nonce = parts[0];
            String timestampStr = parts[1];
            String signature = parts[2];

            // 서명 검증 (constant-time 비교로 타이밍 공격 방지)
            String payload = nonce + "." + timestampStr;
            String expectedSignature = hmacSign(payload);
            if (!MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8))) {
                log.warn("[카카오 OAuth] state 서명 불일치");
                return false;
            }

            // 만료 시간 확인 (5분)
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

    /**
     * HMAC-SHA256 서명
     */
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

    /**
     * 요청 쿠키에서 nonce 추출
     */
    private String extractCookieNonce(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (STATE_NONCE_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * nonce 쿠키 삭제 (일회성 보장)
     */
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