package com.ott.core.modules.user.dto.response;

import com.ott.common.persistence.entity.User;
import com.ott.common.persistence.enums.BanStatus;
import com.ott.common.persistence.enums.UserRole;

import java.time.OffsetDateTime;

public record UserDetailResponse(
        Long userId,
        String email,
        String nickname,
        String profileImageUrl,
        UserRole role,
        String oauthProvider,
        String oauthId,
        BanStatus banned,
        OffsetDateTime bannedUntil,
        String banReason,
        OffsetDateTime bannedAt,
        Long bannedBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static UserDetailResponse from(User user) {
        return new UserDetailResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getRole(),
                user.getOauthProvider(),
                user.getOauthId(),
                user.getBanned(),
                user.getBannedUntil(),
                user.getBanReason(),
                user.getBannedAt(),
                user.getBannedBy(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}