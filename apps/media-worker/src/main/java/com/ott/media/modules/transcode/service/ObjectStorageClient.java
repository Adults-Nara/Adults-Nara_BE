package com.ott.media.modules.transcode.service;

import java.nio.file.Path;

public interface ObjectStorageClient {
    void downloadToFile(String bucket, String objectKey, Path destination);

    void uploadFile(String bucket, String objectKey, Path sourceFile, String contentType);

    void uploadDirectory(String bucket, String baseKey, Path localDir);
}
