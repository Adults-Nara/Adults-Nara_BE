package com.ott.core.modules.auth.controller;

import com.ott.common.error.BusinessException;
import com.ott.common.error.ErrorCode;
import com.ott.common.response.ApiResponse;
import com.ott.core.modules.auth.dto.BackofficeLoginRequest;
import com.ott.core.modules.auth.dto.BackofficeLoginResponse;
import com.ott.core.modules.auth.dto.BackofficeSignupRequest;
import com.ott.core.modules.auth.dto.TokenRefreshResponse;
import com.ott.core.modules.auth.service.BackofficeAuthService;
import com.ott.core.modules.user.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@Slf4j
@RestController
@RequestMapping("/api/v1/backoffice/auth")
@RequiredArgsConstructor
@Tag(name = "백오피스 인증 API", description = "업로더/관리자 로그인 및 회원가입")
public class BackofficeAuthController {

    private static final String REFRESH_TOKEN_COOKIE = "backoffice_refresh_token";
    private static final int REFRESH_TOKEN_COOKIE_MAX_AGE = 90 * 24 * 60 * 60; // 90일

    private final BackofficeAuthService backofficeAuthService;

    /**
     * 백오피스 로그인 (이메일 + 비밀번호)
     *
     * 카카오 로그인과 동일한 토큰 정책:
     * - Access Token → Response body
     * - Refresh Token → HttpOnly 쿠키 (backoffice_refresh_token)
     */
    @Operation(
            summary = "백오피스 로그인",
            description = "이메일과 비밀번호로 백오피스에 로그인합니다. UPLOADER 또는 ADMIN 계정만 가능합니다. " +
                    "AccessToken은 body에, RefreshToken은 HttpOnly 쿠키(backoffice_refresh_token)로 전달됩니다."
    )
    @PostMapping("/login")
    public ApiResponse<BackofficeLoginResponse> login(
            @Valid @RequestBody BackofficeLoginRequest request,
            HttpServletResponse response
    ) {
        BackofficeLoginResponse loginResponse = backofficeAuthService.login(request);

        // Refresh Token → HttpOnly 쿠키 (카카오 로그인과 동일한 쿠키 정책)
        ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, loginResponse.refreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/api/v1/backoffice/auth")
                .maxAge(REFRESH_TOKEN_COOKIE_MAX_AGE)
                .sameSite("None")
                .build();
        response.addHeader("Set-Cookie", refreshCookie.toString());

        log.info("[백오피스 로그인] 쿠키 발급 완료 - userId: {}, role: {}", loginResponse.userId(), loginResponse.role());

        // @JsonIgnore 로 refreshToken 필드는 JSON 직렬화에서 자동 제외됨
        return ApiResponse.success(loginResponse);
    }

    /**
     * Access Token 재발급
     * 쿠키의 backoffice_refresh_token으로 새로운 Access Token 발급
     */
    @Operation(
            summary = "Access Token 재발급",
            description = "쿠키의 backoffice_refresh_token으로 새로운 AccessToken을 발급합니다."
    )
    @PostMapping("/token/refresh")
    public ApiResponse<TokenRefreshResponse> refreshToken(HttpServletRequest request) {
        String refreshToken = extractCookieValue(request, REFRESH_TOKEN_COOKIE);

        if (refreshToken == null) {
            log.warn("[백오피스 토큰 갱신] backoffice_refresh_token 쿠키 없음");
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        TokenRefreshResponse tokenResponse = backofficeAuthService.refreshAccessToken(refreshToken);
        return ApiResponse.success(tokenResponse);
    }

    /**
     * 백오피스 로그아웃
     * Redis의 Refresh Token 삭제 + 쿠키 만료 처리
     */
    @Operation(
            summary = "로그아웃",
            description = "서버의 RefreshToken을 무효화하고 쿠키를 삭제합니다."
    )
    @PostMapping("/logout")
    public ApiResponse<?> logout(Authentication authentication, HttpServletResponse response) {
        Long userId = Long.parseLong(authentication.getName());
        backofficeAuthService.logout(userId);
        expireRefreshTokenCookie(response);
        return ApiResponse.success();
    }

    /**
     * 업로더 회원가입
     */
    @Operation(
            summary = "업로더 회원가입",
            description = "이메일, 비밀번호, 닉네임으로 업로더 계정을 생성합니다. 이메일 중복 체크를 수행합니다."
    )
    @PostMapping("/signup/uploader")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> signupUploader(
            @Valid @RequestBody BackofficeSignupRequest request
    ) {
        UserResponse response = backofficeAuthService.signupUploader(request);
        return ApiResponse.success(response);
    }

    /**
     * 업로더 계정 탈퇴 (Soft Delete)
     * 탈퇴 시 Redis Refresh Token도 함께 삭제
     */
    @Operation(
            summary = "업로더 계정 탈퇴",
            description = "업로더 본인이 계정을 탈퇴합니다. Soft Delete로 처리되며, RefreshToken도 즉시 무효화됩니다."
    )
    @DeleteMapping("/account")
    @PreAuthorize("hasRole('UPLOADER')")
    public ApiResponse<?> deleteAccount(
            Authentication authentication,
            HttpServletResponse response
    ) {
        Long userId = Long.parseLong(authentication.getName());
        backofficeAuthService.deleteUploaderAccount(userId);
        expireRefreshTokenCookie(response);
        return ApiResponse.success();
    }

    /**
     * 이메일 중복 체크
     */
    @Operation(
            summary = "이메일 중복 체크",
            description = "회원가입 전 이메일 사용 가능 여부를 확인합니다. true = 사용 가능, false = 이미 존재"
    )
    @GetMapping("/check-email")
    public ApiResponse<Boolean> checkEmailAvailable(@RequestParam String email) {
        boolean available = !backofficeAuthService.isEmailExists(email);
        return ApiResponse.success(available);
    }

    // ====== Private Methods ======

    /**
     * Refresh Token 쿠키 만료 처리
     * logout, deleteAccount에서 공통으로 사용
     */
    private void expireRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie expiredCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(true)
                .path("/api/v1/backoffice/auth")
                .maxAge(0)
                .sameSite("None")
                .build();
        response.addHeader("Set-Cookie", expiredCookie.toString());
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
}