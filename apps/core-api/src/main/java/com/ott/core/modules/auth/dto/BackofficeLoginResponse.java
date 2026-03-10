package com.ott.core.modules.auth.dto;

import com.ott.common.persistence.entity.User;
import com.ott.common.persistence.enums.UserRole;

/**
 * 백오피스 로그인 성공 응답 (업로더/관리자용)
 *
 * RefreshToken은 HttpOnly 쿠키로 전달되므로 body에 포함하지 않습니다.
 * 내부 처리용 refreshToken 필드는 쿠키 세팅 후 클라이언트에 노출하지 않습니다.
 */
public record BackofficeLoginResponse(
        Long userId,
        String email,
        String nickname,
        String profileImageUrl,
        UserRole role,
        String accessToken,
        String refreshToken   // 내부 처리용 (쿠키 세팅 후 클라이언트에는 노출 안 함)
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

    /**
     * 클라이언트 응답용: refreshToken 제외한 객체 반환
     * RefreshToken은 Set-Cookie 헤더로 별도 전달
     */
    public BackofficeLoginResponse withoutRefreshToken() {
        return new BackofficeLoginResponse(
                this.userId,
                this.email,
                this.nickname,
                this.profileImageUrl,
                this.role,
                this.accessToken,
                null
        );
    }
}