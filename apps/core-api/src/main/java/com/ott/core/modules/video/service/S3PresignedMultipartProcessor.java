package com.ott.core.modules.video.service;

import com.ott.common.error.BusinessException;
import com.ott.common.error.ErrorCode;
import com.ott.core.modules.video.dto.multipart.CompletedPartDto;
import com.ott.core.modules.video.dto.multipart.MultipartInitResult;
import com.ott.core.modules.video.dto.multipart.PresignedPart;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedUploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class S3PresignedMultipartProcessor implements PresignedMultipartProcessor {
    private static final long DEFAULT_TTL_SECONDS = 30 * 60; // 30분
    private static final int DEFAULT_PART_SIZE = 8 * 1024 * 1024; // 기본 파트 사이즈
    private static final long MAX_OBJECT_SIZE_BYTES = DEFAULT_PART_SIZE * 10000L;

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    public S3PresignedMultipartProcessor(S3Client s3Client, S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    @Override
    public MultipartInitResult initMultipart(Long videoId, String bucket, String objectKey, String contentType, long sizeBytes) {
        if (sizeBytes <= 0) throw new BusinessException(ErrorCode.VIDEO_INVALID_SIZE);
        if (sizeBytes > MAX_OBJECT_SIZE_BYTES) throw new BusinessException(ErrorCode.VIDEO_INVALID_SIZE);

        int partSize = DEFAULT_PART_SIZE;
        int partCount = (int) Math.ceil((double) sizeBytes / (double) partSize);

        // 1) CreateMultipartUpload
        CreateMultipartUploadRequest createRequest = CreateMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(contentType)
                .build();

        CreateMultipartUploadResponse createResponse = s3Client.createMultipartUpload(createRequest);
        String uploadId = createResponse.uploadId();

        // 2) PResign each part URL
        List<PresignedPart> presignedParts = new ArrayList<>(partCount);
        long expiresAt = Instant.now().getEpochSecond() + DEFAULT_TTL_SECONDS;

        for (int partNumber = 1; partNumber <= partCount; partNumber++) {
            UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .uploadId(uploadId)
                    .partNumber(partNumber)
                    .build();

            UploadPartPresignRequest presignRequest = UploadPartPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(DEFAULT_TTL_SECONDS))
                    .uploadPartRequest(uploadPartRequest)
                    .build();

            PresignedUploadPartRequest presignedUploadPartRequest = s3Presigner.presignUploadPart(presignRequest);

            presignedParts.add(new PresignedPart(partNumber, presignedUploadPartRequest.url().toString()));
        }

        return new MultipartInitResult(videoId, objectKey, uploadId, partSize, expiresAt, presignedParts);
    }

    @Override
    public void completeMultipart(String bucket, String objectKey, String uploadId, List<CompletedPartDto> parts) {
        if (parts == null || parts.isEmpty()) throw new BusinessException(ErrorCode.INVALID_REQUEST);

        // 1) CompleteMultipartUpload는 partNumber 오름차순이 안전
        List<CompletedPartDto> sorted = parts.stream()
                .sorted(Comparator.comparingInt(CompletedPartDto::partNumber))
                .toList();

        List<CompletedPart> awsParts = new ArrayList<>(sorted.size());
        for (CompletedPartDto p : sorted) {
            awsParts.add(CompletedPart.builder()
                    .partNumber(p.partNumber())
                    .eTag(stripQuotesIfNeeded(p.eTag()))
                    .build());
        }

        CompletedMultipartUpload completed = CompletedMultipartUpload.builder()
                .parts(awsParts)
                .build();

        CompleteMultipartUploadRequest request = CompleteMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .uploadId(uploadId)
                .multipartUpload(completed)
                .build();

        s3Client.completeMultipartUpload(request);
    }

    @Override
    public void abortMultipart(String bucket, String objectKey, String uploadId) {
        AbortMultipartUploadRequest request = AbortMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .uploadId(uploadId)
                .build();

        s3Client.abortMultipartUpload(request);
    }

    private String stripQuotesIfNeeded(String eTag) {
        // 일부 클라이언트가 ETag를 "\"xxxx\""처럼 따옴표 포함으로 넘김
        if (eTag == null) return null;
        String t = eTag.trim();
        if (t.startsWith("\"") && t.endsWith("\"") && t.length() >= 2) {
            return t.substring(1, t.length() - 1);
        }
        return t;
    }
}
