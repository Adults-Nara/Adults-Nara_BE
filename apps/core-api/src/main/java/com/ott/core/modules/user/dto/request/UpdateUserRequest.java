package com.ott.core.modules.user.dto.request;

import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateUserRequest(

        @Size(min = 2, max = 20, message = "닉네임은 2~20자여야 합니다.")
        String nickname

) {}