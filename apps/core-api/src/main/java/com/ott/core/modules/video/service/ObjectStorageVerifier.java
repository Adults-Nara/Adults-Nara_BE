package com.ott.core.modules.video.service;

public interface ObjectStorageVerifier {
    void verifyExistsAndSize(String bucket, String key, long expectedSizeBytes);
}
