package com.ott.core.modules.auth.dto;

/**
 * Access Token 재발급 응답
 */
public record TokenRefreshResponse(
        String accessToken
) {
}