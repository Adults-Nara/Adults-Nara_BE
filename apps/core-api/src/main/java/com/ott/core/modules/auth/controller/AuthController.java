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
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 인증 컨트롤러 (카카오 OAuth + JWT 토큰 관리)
 *
 * CSRF state 검증을 HMAC 서명 기반 stateless 방식으로 처리합니다.
 * state 구조: {nonce}.{timestamp}.{signature}
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "인증 API", description = "카카오 OAuth 로그인 및 토큰 관리")
public class AuthController {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final long STATE_EXPIRY_SECONDS = 300; // 5분

    private final AuthService authService;
    private final String kakaoClientId;
    private final String kakaoRedirectUri;
    private final byte[] stateSigningKey;

    public AuthController(
            AuthService authService,
            @Value("${oauth2.kakao.client-id}") String kakaoClientId,
            @Value("${oauth2.kakao.redirect-uri}") String kakaoRedirectUri,
            @Value("${jwt.secret}") String jwtSecret
    ) {
        this.authService = authService;
        this.kakaoClientId = kakaoClientId;
        this.kakaoRedirectUri = kakaoRedirectUri;
        this.stateSigningKey = Base64.getDecoder().decode(jwtSecret);
    }

    /**
     * 카카오 로그인 URL 조회
     */
    @Operation(
            summary = "카카오 로그인 URL 조회",
            description = "프론트엔드에서 카카오 로그인 페이지로 이동할 URL을 반환합니다. " +
                    "CSRF 방지를 위한 HMAC 서명 기반 state 파라미터가 포함됩니다."
    )
    @GetMapping("/kakao/login-url")
    public ApiResponse<String> getKakaoLoginUrl() {
        String state = generateSignedState();

        String loginUrl = "https://kauth.kakao.com/oauth/authorize"
                + "?client_id=" + kakaoClientId
                + "&redirect_uri=" + URLEncoder.encode(kakaoRedirectUri, StandardCharsets.UTF_8)
                + "&response_type=code"
                + "&state=" + state;

        return ApiResponse.success(loginUrl);
    }

    /**
     * 카카오 OAuth 콜백 처리
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
            @RequestParam(value = "state", required = false) String state
    ) {
        if (state == null || !verifySignedState(state)) {
            log.warn("[카카오 OAuth] state 검증 실패 - CSRF 공격 의심");
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        log.info("[카카오 OAuth] 콜백 수신 - 인가코드 길이: {}", code.length());
        LoginResponse response = authService.kakaoLogin(code, state);
        return ApiResponse.success(response);
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
        UserDetailResponse response = authService.getCurrentUser(userId);
        return ApiResponse.success(response);
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
        TokenRefreshResponse response = authService.refreshAccessToken(request.refreshToken());
        return ApiResponse.success(response);
    }

    // ====== Private Methods ======

    /**
     * HMAC 서명 기반 state 토큰 생성 (세션 저장 불필요)
     * 형식: {nonce}.{timestamp}.{signature}
     */
    private String generateSignedState() {
        byte[] nonceBytes = new byte[16];
        SECURE_RANDOM.nextBytes(nonceBytes);
        String nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(nonceBytes);

        long timestamp = System.currentTimeMillis() / 1000;
        String payload = nonce + "." + timestamp;
        String signature = hmacSign(payload);

        return payload + "." + signature;
    }

    /**
     * state 토큰 검증 (서명 + 만료 시간)
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

            // 서명 검증
            String payload = nonce + "." + timestampStr;
            if (!hmacSign(payload).equals(signature)) {
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
        } catch (Exception e) {
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
        } catch (Exception e) {
            throw new RuntimeException("HMAC 서명 생성 실패", e);
        }
    }
}