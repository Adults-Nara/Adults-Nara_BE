package com.ott.core.modules.video.service;

public interface PresignedUrlProcessor {
    PresignedPutUrlResult presignPut(String bucket, String objectKey, String contentType, long ttlSeconds);

    record PresignedPutUrlResult(String url, long expiresAtEpochSeconds) {}
}
