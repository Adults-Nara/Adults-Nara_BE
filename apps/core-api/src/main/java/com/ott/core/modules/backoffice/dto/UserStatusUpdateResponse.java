package com.ott.core.modules.backoffice.dto;

import java.util.List;

public record UserStatusUpdateResponse(
        List<String> userIds
) {
}
