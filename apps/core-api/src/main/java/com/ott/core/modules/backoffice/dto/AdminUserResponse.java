package com.ott.core.modules.backoffice.dto;

import com.ott.common.persistence.enums.BanStatus;

import java.time.OffsetDateTime;

public record AdminUserResponse(
        String userId,
        String profileImageUrl,
        String nickname,
        String email,
        BanStatus banStatus,
        OffsetDateTime createdAt
) {
}
