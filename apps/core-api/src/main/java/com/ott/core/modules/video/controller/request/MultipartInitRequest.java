package com.ott.core.modules.video.controller.request;

public record MultipartInitRequest(
        String contentType,
        long sizeBytes
) {
}
