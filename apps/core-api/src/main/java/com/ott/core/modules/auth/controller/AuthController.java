package com.ott.core.modules.auth.controller;

import com.ott.common.error.BusinessException;
import com.ott.common.error.ErrorCode;
import com.ott.common.response.ApiResponse;
import com.ott.core.modules.auth.dto.LoginResponse;
import com.ott.core.modules.auth.dto.TokenRefreshRequest;
import com.ott.core.modules.auth.dto.TokenRefreshResponse;
import com.ott.core.modules.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "인증 API", description = "카카오 OAuth 로그인 및 토큰 관리")
public class AuthController {

    private static final String OAUTH_STATE_SESSION_KEY = "oauth_state";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AuthService authService;

    @Value("${oauth2.kakao.client-id}")
    private String kakaoClientId;

    @Value("${oauth2.kakao.redirect-uri}")
    private String kakaoRedirectUri;

    /**
     * 카카오 로그인 URL 조회
     *
     * [Fix #4] state 파라미터를 추가하여 OAuth CSRF 공격을 방지합니다.
     * state 값은 세션에 저장하고, 콜백 시 검증합니다.
     */
    @Operation(
            summary = "카카오 로그인 URL 조회",
            description = "프론트엔드에서 카카오 로그인 페이지로 이동할 URL을 반환합니다. " +
                    "CSRF 방지를 위한 state 파라미터가 포함됩니다."
    )
    @GetMapping("/kakao/login-url")
    public ApiResponse<String> getKakaoLoginUrl(HttpSession session) {
        // [Fix #4] CSRF 방지용 state 토큰 생성
        String state = generateStateToken();
        session.setAttribute(OAUTH_STATE_SESSION_KEY, state);

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
     * [Fix #4] state 파라미터를 검증하여 CSRF 공격을 방지합니다.
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
            HttpSession session
    ) {
        // [Fix #4] state 파라미터 검증
        String savedState = (String) session.getAttribute(OAUTH_STATE_SESSION_KEY);
        if (savedState != null) {
            if (state == null || !savedState.equals(state)) {
                log.warn("[카카오 OAuth] state 불일치 - CSRF 공격 의심. expected: {}, actual: {}", savedState, state);
                throw new BusinessException(ErrorCode.UNAUTHORIZED);
            }
            // 검증 완료 후 세션에서 제거 (재사용 방지)
            session.removeAttribute(OAUTH_STATE_SESSION_KEY);
        }

        log.info("[카카오 OAuth] 콜백 수신 - 인가코드 길이: {}", code.length());
        LoginResponse response = authService.kakaoLogin(code, state);
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
     * [Fix #4] OAuth state 토큰 생성
     * 32바이트 랜덤 값을 Base64 URL-safe로 인코딩
     */
    private String generateStateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}