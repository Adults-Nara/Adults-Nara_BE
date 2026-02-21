package com.ott.core.modules.video.service;

import com.ott.common.error.BusinessException;
import com.ott.common.error.ErrorCode;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

@Component
public class S3ObjectStorageVerifier implements ObjectStorageVerifier {
    private final S3Client s3Client;

    public S3ObjectStorageVerifier(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public void verifyExistsAndSize(String bucket, String key, long expectedSizeBytes) {
        HeadObjectResponse head;
        try {
            head = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
        } catch (NoSuchKeyException e) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        long actualSize = head.contentLength();
        if (actualSize <= 0) throw new BusinessException(ErrorCode.NOT_FOUND);
        if (expectedSizeBytes > 0 && actualSize != expectedSizeBytes) {
            throw new BusinessException(ErrorCode.VIDEO_INVALID_SIZE);
        }
    }
}
