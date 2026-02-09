package com.ott.core.modules.video.dto.multipart;

public record CompletedPartDto(
        int partNumber,
        String eTag
) {
}
