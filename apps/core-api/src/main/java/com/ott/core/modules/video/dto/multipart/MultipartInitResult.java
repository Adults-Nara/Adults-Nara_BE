package com.ott.core.modules.video.dto.multipart;

import java.util.List;

public record MultipartInitResult(
        Long videoId,
        String objectKey,
        String uploadId,
        int partSizeBytes,
        long expiresAtEpochSeconds,
        List<PresignedPart> presignedParts
) {}
