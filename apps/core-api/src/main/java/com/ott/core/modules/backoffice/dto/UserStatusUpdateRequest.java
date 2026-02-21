package com.ott.core.modules.backoffice.dto;

import com.ott.common.persistence.enums.BanStatus;

import java.util.List;

public record UserStatusUpdateRequest(
        List<Long> userIds,
        BanStatus banStatus,
        String banReason
) {
}
