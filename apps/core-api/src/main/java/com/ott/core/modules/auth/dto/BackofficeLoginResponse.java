package com.ott.core.modules.auth.dto;

import com.ott.common.persistence.entity.User;
import com.ott.common.persistence.enums.UserRole;

/**
 * 백오피스 로그인 성공 응답 (업로더/관리자용)
 */
public record BackofficeLoginResponse(
        Long userId,
        String email,
        String nickname,
        String profileImageUrl,
        UserRole role,
        String accessToken,
        String refreshToken
) {
    public static BackofficeLoginResponse of(User user, String accessToken, String refreshToken) {
        return new BackofficeLoginResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getRole(),
                accessToken,
                refreshToken
        );
    }
}