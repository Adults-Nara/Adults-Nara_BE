package com.ott.core.modules.auth.dto;

import com.ott.common.persistence.enums.UserRole;

/**
 * 로그인 성공 시 프론트엔드에 전달하는 응답
 * RefreshToken은 HttpOnly 쿠키로 전달되므로 body에는 포함하지 않음
 */
public record LoginResponse(
        Long userId,
        String email,
        String nickname,
        String profileImageUrl,
        UserRole role,
        String accessToken,
        String refreshToken,   // 내부 처리용 (쿠키 세팅 후 클라이언트에는 노출 안 함)
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

    /**
     * 클라이언트 응답용: refreshToken 제외한 객체 반환
     * RefreshToken은 Set-Cookie 헤더로 별도 전달
     */
    public LoginResponse withoutRefreshToken() {
        return new LoginResponse(
                this.userId,
                this.email,
                this.nickname,
                this.profileImageUrl,
                this.role,
                this.accessToken,
                null,           // body에서 제거
                this.isNewUser
        );
    }
}