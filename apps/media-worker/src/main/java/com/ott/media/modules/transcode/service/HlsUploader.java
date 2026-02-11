package com.ott.media.modules.transcode.service;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

@Component
public class HlsUploader {
    private final ObjectStorageClient storage;

    public HlsUploader(ObjectStorageClient storage) {
        this.storage = storage;
    }

    public void uploadHlsDirectory(String bucket, String outputBaseKey, Path localRoot) {
        // 1) seg_* 먼저 업로드
        uploadByGlob(bucket, outputBaseKey, localRoot, "seg_*.ts",
                "video/mp2t");

        // 2) variant playlist 업로드
        uploadByName(bucket, outputBaseKey, localRoot, "playlist.m3u8",
                "application/vnd.apple.mpegurl");

        // 3) master 업로드
        Path master = localRoot.resolve("master.m3u8");
        storage.uploadFile(bucket, outputBaseKey + "master.m3u8", master,
                "application/vnd.apple.mpegurl");
    }

    private void uploadByGlob(String bucket, String baseKey, Path root, String pattern, String contentType) {
        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().matches(globToRegex(pattern)))
                    .forEach(p -> {
                        String key = baseKey + root.relativize(p).toString().replace("\\", "/");
                        storage.uploadFile(bucket, key, p, contentType);
                    });
        } catch (IOException e) {
            throw new RuntimeException("uploadByGlob failed", e);
        }
    }

    private void uploadByName(String bucket, String baseKey, Path root, String fileName, String contentType) {
        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().equals(fileName))
                    .sorted(Comparator.comparing(Path::toString)) // 예: 360p/playlist.m3u8 → 720p → ...
                    .forEach(p -> {
                        String key = baseKey + root.relativize(p).toString().replace("\\", "/");
                        storage.uploadFile(bucket, key, p, contentType);
                    });
        } catch (IOException e) {
            throw new RuntimeException("uploadByName failed", e);
        }
    }

    private String globToRegex(String glob) {
        // 아주 단순 변환: seg_*.ts 용도
        return glob.replace(".", "\\.").replace("*", ".*");
    }
}
