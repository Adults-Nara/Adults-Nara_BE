package com.ott.core.modules.usertag.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

public record OnboardingTagRequest(
        @Size(min = 5, max = 5, message = "태그는 5개를 선택해주세요.")
        List<Long> tagIds
) {
}
