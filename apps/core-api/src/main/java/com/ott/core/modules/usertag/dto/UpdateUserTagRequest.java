package com.ott.core.modules.usertag.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateUserTagRequest(
        @NotNull(message = "태그 ID 목록은 null일 수 없습니다.")
        List<Long> tagIds
) {
}
