package com.ott.core.modules.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ott.common.persistence.entity.User;
import com.ott.common.persistence.enums.UserRole;

/**
 * 백오피스 로그인 성공 응답 (업로더/관리자용)
 *
 * RefreshToken은 HttpOnly 쿠키로 전달되므로 JSON 직렬화에서 제외됩니다.
 * 내부적으로는 컨트롤러에서 쿠키 세팅 후 클라이언트에 노출되지 않습니다.
 */
public record BackofficeLoginResponse(
        Long userId,
        String email,
        String nickname,
        String profileImageUrl,
        UserRole role,
        String accessToken,
        @JsonIgnore String refreshToken   // JSON 직렬화 제외 - Set-Cookie 헤더로만 전달
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