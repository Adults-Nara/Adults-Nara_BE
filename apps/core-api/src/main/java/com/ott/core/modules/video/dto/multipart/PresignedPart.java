package com.ott.core.modules.video.dto.multipart;

public record PresignedPart(
        int partNumber,
        String url
) {
}
