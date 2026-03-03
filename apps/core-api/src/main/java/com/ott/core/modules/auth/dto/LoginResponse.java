package com.ott.core.modules.auth.dto;

import com.ott.common.persistence.enums.UserRole;

public record LoginResponse(
        Long userId,
        String email,
        String nickname,
        String profileImageUrl,
        UserRole role,
        String accessToken,
        String refreshToken,
        boolean isNewUser,
        boolean onboardingCompleted   // 추가
) {
    public static LoginResponse of(com.ott.common.persistence.entity.User user,
                                   String accessToken,
                                   String refreshToken,
                                   boolean isNewUser) {
        return new LoginResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getRole(),
                accessToken,
                refreshToken,
                isNewUser,
                user.isOnboardingCompleted()   // 추가
        );
    }

    public LoginResponse withoutRefreshToken() {
        return new LoginResponse(
                this.userId,
                this.email,
                this.nickname,
                this.profileImageUrl,
                this.role,
                this.accessToken,
                null,
                this.isNewUser,
                this.onboardingCompleted       // 추가
        );
    }
}