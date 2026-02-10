package com.ott.core.modules.user.dto.request;

import com.ott.common.persistence.enums.BanStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BanUserRequest(
        @NotNull(message = "정지 상태는 필수입니다.")
        BanStatus banStatus,

        @NotBlank(message = "정지 사유는 필수입니다.")
        String reason
) {
    public BanUserRequest {
        if (banStatus == BanStatus.ACTIVE ||
                banStatus == BanStatus.DEACTIVATED ||
                banStatus == BanStatus.DELETED) {
            throw new IllegalArgumentException("유효하지 않은 정지 상태입니다.");
        }
    }
}