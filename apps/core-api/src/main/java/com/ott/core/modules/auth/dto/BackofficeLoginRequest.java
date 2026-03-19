package com.ott.core.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record BackofficeLoginRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(
                regexp = "^[a-zA-Z0-9]+@[a-zA-Z]+\\.[a-zA-Z]{2,}$",
                message = "유효한 이메일 형식이 아닙니다."
        )
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        String password
) {
}