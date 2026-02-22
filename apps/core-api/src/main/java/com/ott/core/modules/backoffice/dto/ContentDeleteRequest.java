package com.ott.core.modules.backoffice.dto;

import java.util.List;

public record ContentDeleteRequest(
        List<Long> videoMetadataIds
) {
}
