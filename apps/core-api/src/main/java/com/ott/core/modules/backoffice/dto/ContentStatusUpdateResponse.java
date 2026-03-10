package com.ott.core.modules.backoffice.dto;

import java.util.List;

public record ContentStatusUpdateResponse(
        List<String> videoIds
) {
}
