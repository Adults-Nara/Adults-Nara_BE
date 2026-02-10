package com.ott.core.modules.user.dto.response;

import com.ott.common.persistence.entity.User;
import com.ott.common.persistence.enums.BanStatus;
import com.ott.common.persistence.enums.UserRole;

import java.time.OffsetDateTime;

public record UserResponse(
        Long userId,
        String email,
        String nickname,
        String profileImageUrl,
        UserRole role,
        BanStatus banned,
        OffsetDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getRole(),
                user.getBanned(),
                user.getCreatedAt()
        );
    }
}