package com.ott.media.modules.transcode.service;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@Component
public class S3ObjectStorageClient implements ObjectStorageClient {
    private final S3Client s3;

    public S3ObjectStorageClient(S3Client s3) {
        this.s3 = s3;
    }

    @Override
    public void downloadToFile(String bucket, String objectKey, Path destination) {
        try {
            Files.createDirectories(destination.getParent());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create parent dir: " + destination, e);
        }

        GetObjectRequest req = GetObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();

        s3.getObject(req, destination);
    }

    @Override
    public void uploadFile(String bucket, String objectKey, Path sourceFile, String contentType) {
        PutObjectRequest req = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(contentType)
                .build();

        s3.putObject(req, RequestBody.fromFile(sourceFile));
    }

    @Override
    public void uploadDirectory(String bucket, String baseKey, Path localDir) {
        if (!Files.isDirectory(localDir)) {
            throw new IllegalArgumentException("Not a directory: " + localDir);
        }

        try (Stream<Path> paths = Files.walk(localDir)) {
            paths
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        String relative = localDir.relativize(file).toString()
                                .replace("\\", "/"); // Windows 대응

                        String objectKey = baseKey + relative;

                        String contentType = guessContentType(file);

                        uploadFile(bucket, objectKey, file, contentType);
                    });
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload directory: " + localDir, e);
        }
    }

    private String guessContentType(Path file) {
        String name = file.getFileName().toString();
        if (name.endsWith(".m3u8")) return "application/vnd.apple.mpegurl";
        if (name.endsWith(".ts")) return "video/mp2t";
        if (name.endsWith(".mp4")) return "video/mp4";
        return "application/octet-stream";
    }
}
