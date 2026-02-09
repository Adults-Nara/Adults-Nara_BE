package com.ott.core.modules.video.service;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.Instant;

@Component
public class S3PresignedUrlProcessor implements PresignedUrlProcessor {
    private final S3Presigner presigner;

    public S3PresignedUrlProcessor(S3Presigner presigner) {
        this.presigner = presigner;
    }

    @Override
    public PresignedPutUrlResult presignPut(String bucket, String objectKey, String contentType, long ttlSeconds) {
        if (bucket == null || bucket.isBlank()) throw new IllegalArgumentException("bucket is blank");
        if (objectKey == null || objectKey.isBlank()) throw new IllegalArgumentException("objectKey is blank");
        if (ttlSeconds <= 0) throw new IllegalArgumentException("ttlSeconds must be > 0");

        // 1) 실제 PubObjectRequest를 먼저 만든다.
        // 여기서 contentType을 넣으면 "해당 헤더 포함"이 서명에 반영됨 → 다른 Content-Type으로 PUT하면 403
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(contentType)
                .build();

        // 2) Presign 요청: 만료(Duration) 지정
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(ttlSeconds))
                .putObjectRequest(putObjectRequest)
                .build();

        // 3) Presigned URL 생성
        PresignedPutObjectRequest presigned = presigner.presignPutObject(presignRequest);

        // presigned.url()은 java.net.URL
        String url = presigned.url().toString();

        // 만료 시각 계산(대략). SDK 내부 만료와 동일하게 맞추려면 now + ttl로 충분
        long expiresAt = Instant.now().getEpochSecond() + ttlSeconds;

        if (presigned.httpRequest().method() != SdkHttpMethod.PUT) {
            throw new IllegalStateException("Expected PUT presign, got " + presigned.httpRequest().method());
        }

        return new PresignedPutUrlResult(url, expiresAt);
    }
}
