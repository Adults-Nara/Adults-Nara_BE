package com.ott.core.modules.auth.controller;

import com.ott.common.response.ApiResponse;
import com.ott.core.modules.auth.dto.LoginResponse;
import com.ott.core.modules.auth.dto.TokenRefreshRequest;
import com.ott.core.modules.auth.dto.TokenRefreshResponse;
import com.ott.core.modules.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "인증 API", description = "카카오 OAuth 로그인 및 토큰 관리")
public class AuthController {

    private final AuthService authService;

    @Value("${oauth2.kakao.client-id}")
    private String kakaoClientId;

    @Value("${oauth2.kakao.redirect-uri}")
    private String kakaoRedirectUri;

    /**
     * 카카오 로그인 URL 조회
     *
     * 프론트엔드에서 이 URL로 리다이렉트하면 카카오 로그인 페이지가 표시됩니다.
     * 사용자가 동의하면 카카오가 redirect_uri로 인가코드(code)를 전달합니다.
     */
    @Operation(
            summary = "카카오 로그인 URL 조회",
            description = "프론트엔드에서 카카오 로그인 페이지로 이동할 URL을 반환합니다."
    )
    @GetMapping("/kakao/login-url")
    public ApiResponse<String> getKakaoLoginUrl() {
        String loginUrl = "https://kauth.kakao.com/oauth/authorize"
                + "?client_id=" + kakaoClientId
                + "&redirect_uri=" + kakaoRedirectUri
                + "&response_type=code";

        return ApiResponse.success(loginUrl);
    }

    /**
     * 카카오 OAuth 콜백 처리
     *
     * 카카오 인가 서버가 redirect_uri로 보내주는 인가코드(code)를 받아서:
     * 1. 카카오 액세스 토큰 교환
     * 2. 카카오 사용자 정보 조회
     * 3. 사용자 생성/조회 (Find or Create)
     * 4. JWT 토큰 발급
     *
     * @param code 카카오 인가코드 (카카오가 redirect_uri에 ?code=xxx 로 전달)
     * @return 로그인 응답 (유저정보 + JWT 토큰)
     */
    @Operation(
            summary = "카카오 로그인 (인가코드 → JWT 발급)",
            description = "카카오 인가코드를 받아 사용자 인증 후 JWT 토큰을 발급합니다."
    )
    @GetMapping("/kakao/callback")
    public ApiResponse<LoginResponse> kakaoCallback(
            @Parameter(description = "카카오 인가코드", required = true)
            @RequestParam("code") String code
    ) {
        log.info("[카카오 OAuth] 콜백 수신 - 인가코드 길이: {}", code.length());
        LoginResponse response = authService.kakaoLogin(code);
        return ApiResponse.success(response);
    }

    /**
     * Access Token 재발급
     *
     * Refresh Token을 사용하여 만료된 Access Token을 재발급합니다.
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
}