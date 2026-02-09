package com.ott.core.modules.video.controller.request;

import com.ott.core.modules.video.dto.multipart.CompletedPartDto;

import java.util.List;

public record MultipartCompleteRequest(
        String uploadId,
        List<CompletedPartDto> parts,
        long sizeBytes
) {
}
