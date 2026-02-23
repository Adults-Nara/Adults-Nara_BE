package com.ott.core.modules.auth.dto;

import com.ott.common.persistence.enums.UserRole;

/**
 * 로그인 성공 시 프론트엔드에 전달하는 응답
 */
public record LoginResponse(
        Long userId,
        String email,
        String nickname,
        String profileImageUrl,
        UserRole role,
        String accessToken,
        String refreshToken,
        boolean isNewUser
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
                isNewUser
        );
    }
}