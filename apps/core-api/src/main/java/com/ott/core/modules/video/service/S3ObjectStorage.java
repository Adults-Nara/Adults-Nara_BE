package com.ott.core.modules.video.service;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.ArrayList;
import java.util.List;

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

    public void delete(String bucketName, String key) {
        DeleteObjectRequest req = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        s3Client.deleteObject(req);
    }

    public void deleteByPrefix(String bucketName, String prefix) {
        String normalizedPrefix = normalizePrefix(prefix);

        String continuationToken = null;

        do {
            ListObjectsV2Request listReq = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(normalizedPrefix)
                    // S3에서 목록 조회(ListObjectsV2) 결과가 여러 페이지일 때 다음 페이지를 가져오기 위한 커서(cursor)
                    .continuationToken(continuationToken)
                    .build();

            ListObjectsV2Response listRes = s3Client.listObjectsV2(listReq);

            List<ObjectIdentifier> toDelete = new ArrayList<>();
            for (S3Object obj : listRes.contents()) {
                toDelete.add(ObjectIdentifier.builder().key(obj.key()).build());
            }

            // 한 번에 최대 1000개까지 배치 삭제 가능
            if (!toDelete.isEmpty()) {
                DeleteObjectsRequest delReq = DeleteObjectsRequest.builder()
                        .bucket(bucketName)
                        .delete(Delete.builder().objects(toDelete).build())
                        .build();

                s3Client.deleteObjects(delReq);
            }

            continuationToken = listRes.nextContinuationToken();
        } while (continuationToken != null);
    }

    private String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) return "";
        // 폴더로 쓰려면 보통 trailing slash를 붙여서 의도치 않은 매칭을 줄임
        return prefix.endsWith("/") ? prefix : prefix + "/";
    }
}
