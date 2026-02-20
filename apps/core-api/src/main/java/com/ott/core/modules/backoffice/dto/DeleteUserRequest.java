package com.ott.core.modules.backoffice.dto;

import java.util.List;

public record DeleteUserRequest(
        List<Long> userIds
) {
}
