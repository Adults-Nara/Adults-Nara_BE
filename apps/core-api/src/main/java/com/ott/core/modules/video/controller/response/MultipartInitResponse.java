package com.ott.core.modules.video.controller.response;

import com.ott.core.modules.video.dto.multipart.MultipartInitResult;
import com.ott.core.modules.video.dto.multipart.PresignedPart;

import java.util.List;

public record MultipartInitResponse(
        String videoId,
        String objectKey,
        String uploadId,
        int partSizeBytes,
        List<PresignedPart> presignedParts,
        long expiresAtEpochSeconds
) {
    public static MultipartInitResponse of(MultipartInitResult result) {
        return new MultipartInitResponse(
                result.videoId().toString(),
                result.objectKey(),
                result.uploadId(),
                result.partSizeBytes(),
                result.presignedParts(),
                result.expiresAtEpochSeconds()
        );
    }
}
