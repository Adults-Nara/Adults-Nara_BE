package com.ott.media.modules.transcode.service;

import com.ott.common.persistence.entity.Video;
import com.ott.common.persistence.enums.ProcessingStatus;
import com.ott.media.modules.transcode.dto.VideoTranscodeRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Component
public class TranscodingWorkerConsumer {
    private final ObjectStorageClient storage;
    private final FfmpegTranscoder ffmpegTranscoder;
    private final FfprobeMediaProbe ffprobeMediaProbe;
    private final HlsUploader uploader;
    private final VideoUpdater videoUpdater;

    private final String bucket;
    private final String outputBasePrefix = "videos/";
    private final int SEGMENT_SECONDS = 4;
    private final List<Integer> RENDITIONS = List.of(360, 720, 1080);
    private final int ENCODE_VERSION = 1;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public TranscodingWorkerConsumer(ObjectStorageClient storage,
                                     FfmpegTranscoder ffmpegTranscoder,
                                     FfprobeMediaProbe ffprobeMediaProbe,
                                     HlsUploader uploader,
                                     VideoUpdater videoUpdater,
                                     @Value("${aws.s3.source-bucket}") String bucket) {
        this.storage = storage;
        this.ffmpegTranscoder = ffmpegTranscoder;
        this.ffprobeMediaProbe = ffprobeMediaProbe;
        this.uploader = uploader;
        this.videoUpdater = videoUpdater;
        this.bucket = bucket;
    }

    @KafkaListener(topics = "video-transcode-requested", groupId = "transcode-worker-group")
    public void onMessage(VideoTranscodeRequestedEvent evt, Acknowledgment ack) {
        // 작업 디렉터리 준비
        Path workRoot = Paths.get("/tmp/transcode")
                .resolve(evt.videoId().toString())
                .resolve("v" + ENCODE_VERSION);

        try {
            Video video = videoUpdater.readByVideo(evt.videoId());

            // 이미 READY면 스킵 (중복/재처리 방지)
            if (video.getProcessingStatus() == ProcessingStatus.READY) {
                logger.info("[transcode] 이미 READY. skip videoId={}", evt.videoId());
                ack.acknowledge();
                return;
            }

            Files.createDirectories(workRoot);


            // 소스 다운로드
            Path input = workRoot.resolve("source.mp4");
            storage.downloadToFile(bucket, video.getSourceKey(), input);

            // 영상 길이 추출
            int durationSeconds = (int) ffprobeMediaProbe.readDurationSeconds(input);
            videoUpdater.updateVideoDuration(evt.videoId(), durationSeconds);

            // 로컬 HLS 생성
            Path hlsRoot = workRoot.resolve("hls"); // 여기 아래에 360p/720p/...
            ffmpegTranscoder.transcodeToHlsTs(input, hlsRoot, SEGMENT_SECONDS, RENDITIONS);

            // 업로드 (세그먼트 → variant → master 순)
            String outputBaseKey = outputBasePrefix
                    + evt.videoId()
                    + "/outputs/hls/v" + ENCODE_VERSION
                    + "/";

            uploader.uploadHlsDirectory(bucket, outputBaseKey, hlsRoot);

            // 성공 기록
            videoUpdater.updateReady(evt.videoId(), ENCODE_VERSION);

            ack.acknowledge();
        } catch (Exception ex) {
            // 실패 처리
            logger.error("[ffmpeg] 실패 videoId = {} ", evt.videoId(), ex);

            throw new RuntimeException(ex);
        } finally {
            // 8) 로컬 정리(실패해도 정리)
            safeDeleteDirectory(workRoot);
        }
    }

    private void safeDeleteDirectory(Path dir) {
        try {
            if (Files.notExists(dir)) return;
            Files.walk(dir)
                    .sorted((a, b) -> b.compareTo(a)) // 역순 삭제
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (Exception ignored) {}
                    });
        } catch (Exception ignored) {}
    }
}
