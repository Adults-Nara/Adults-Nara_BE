package com.ott.core.modules.usertag.dto;

import java.util.List;

public record UpdateUserTagRequest(
        List<Long> tagIds
) {
}
