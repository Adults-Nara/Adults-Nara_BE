package com.ott.core.modules.user.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(min = 2, max = 20, message = "닉네임은 2~20자여야 합니다.")
        String nickname,

        @Size(min = 8, max = 20, message = "비밀번호는 8~20자여야 합니다.")
        String password,

        String profileImageUrl  // 선택사항 (S3 URL)
) {}