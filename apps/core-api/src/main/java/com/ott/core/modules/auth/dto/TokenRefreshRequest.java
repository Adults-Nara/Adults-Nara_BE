package com.ott.core.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Refresh Token으로 Access Token 재발급 요청
 */
public record TokenRefreshRequest(
        @NotBlank(message = "Refresh Token은 필수입니다.")
        String refreshToken
) {
}