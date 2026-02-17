package com.ott.core.modules.video.service;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
public class S3ObjectStorage {
    private final S3Client s3Client;

    public S3ObjectStorage(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public String save(String bucketName, String key, byte[] bytes, String contentType) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(bytes));
        return key;
    }
}
