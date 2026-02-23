package com.ott.core.modules.user.dto.request;

import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateUserRequest(
        @Size(min = 2, max = 20, message = "닉네임은 2~20자여야 합니다.")
        String nickname,

        @Size(min = 8, max = 20, message = "비밀번호는 8~20자여야 합니다.")
        String password,

        String profileImageUrl,  // 선택사항 (S3 URL)

        /**
         * [신규] 선호 태그 ID 목록
         * 프론트엔드에서 태그 선택 시 태그 ID 리스트를 전달합니다.
         * null이면 선호 태그를 수정하지 않습니다.
         */
        List<Long> preferredTagIds
) {}