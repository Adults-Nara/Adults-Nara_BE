package com.ott.media.modules.transcode.service;

import com.ott.media.modules.transcode.dto.Rendition;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class DefaultFfmpegTranscoder implements FfmpegTranscoder {
    private final ProcessRunner processRunner;
    private final FfprobeMediaProbe ffprobeMediaProbe;

    public DefaultFfmpegTranscoder(ProcessRunner processRunner,
                                   FfprobeMediaProbe ffprobeMediaProbe) {
        this.processRunner = processRunner;
        this.ffprobeMediaProbe = ffprobeMediaProbe;
    }

    @Override
    public void transcodeToHlsTs(Path inputFile, Path outputRoot, int segmentSeconds, List<Integer> renditions) {
        // outputRoot 디렉터리를 만든다.
        try {
            Files.createDirectories(outputRoot);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create outputRoot: " + outputRoot, e);
        }

        double fps = ffprobeMediaProbe.readFps(inputFile);
        runMultiRenditionsHls(inputFile, outputRoot, segmentSeconds, fps);
    }

    private void runSingleRendition(Path inputFile, Path variantDir, Rendition rendition, int segSec, double fps) {
        try {
            Files.createDirectories(variantDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create variantDir: " + variantDir, e);
        }

        String playlist = variantDir.resolve("playlist.m3u8").toString();
        String segments = variantDir.resolve("seg_%05d.ts").toString();

        int gop = calcGop(segSec, fps);

        // GOP/키프레임 정렬 (SEG 초마다 강제)
        String forceKeyFrames = "expr:gte(t,n_forced*" + segSec + ")";

        // 수정 (scale 후 pad로 목표 해상도 채우기 → 항상 짝수/고정 해상도 보장)
        String vf = "scale=w=" + rendition.width() + ":h=" + rendition.height()
                + ":force_original_aspect_ratio=decrease"
                + ",pad=" + rendition.width() + ":" + rendition.height()
                + ":(ow-iw)/2:(oh-ih)/2";

        List<String> cmd = List.of(
                "ffmpeg",
                "-y",
                "-i", inputFile.toString(),

                // 비디오 인코딩
                "-c:v", "libx264",
                "-profile:v", "main",
                "-pix_fmt", "yuv420p",
                "-vf", vf,

                // ABR 안정화를 위한 rate control(예시)
                "-b:v", rendition.bitrateKbps() + "k",
                "-maxrate", rendition.maxrateKbps() + "k",
                "-bufsize", rendition.bufsizeKbps() + "k",

                // 키프레임/씬컷 제어
                "-g", String.valueOf(gop),          // 30fps 가정. (실무에선 입력 fps 읽어서 계산 추천)
                "-keyint_min", String.valueOf(gop),
                "-sc_threshold", "0",
                "-force_key_frames", forceKeyFrames,

                // 오디오(간단히 포함) - 추후 오디오 분리 권장
                "-c:a", "aac",
                "-b:a", "128k",
                "-ac", "2",

                // HLS 출력(TS)
                "-f", "hls",
                "-hls_time", String.valueOf(segSec),
                "-hls_playlist_type", "vod",
                "-hls_segment_filename", segments,
                playlist
        );

        processRunner.run(cmd, variantDir);
    }

    private void runMultiRenditionsHls(Path inputFile, Path outputRoot, int segSec, double fps) {
        try {
            Files.createDirectories(outputRoot);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create outputRoot: " + outputRoot, e);
        }

        int gop = calcGop(segSec, fps);

        String filter = String.join("",
                "[0:v]split=3[v360][v720][v1080];",
                "[v360]scale=640:360:force_original_aspect_ratio=decrease,pad=640:360:(ow-iw)/2:(oh-ih)/2[v360out];",
                "[v720]scale=1280:720:force_original_aspect_ratio=decrease,pad=1280:720:(ow-iw)/2:(oh-ih)/2[v720out];",
                "[v1080]scale=1920:1080:force_original_aspect_ratio=decrease,pad=1920:1080:(ow-iw)/2:(oh-ih)/2[v1080out]"
        );

        String segmentPattern = outputRoot.resolve("%v/seg_%05d.ts").toString();
        String playlistPattern = outputRoot.resolve("%v/playlist.m3u8").toString();

        List<String> cmd = new ArrayList<>(List.of(
                "ffmpeg", "-y",
                "-i", inputFile.toString(),
                "-filter_complex", filter,

                // 360p
                "-map", "[v360out]", "-map", "0:a:0?",
                "-c:v:0", "libx264", "-profile:v:0", "main", "-pix_fmt", "yuv420p",
                "-b:v:0", "900k", "-maxrate:v:0", "1100k", "-bufsize:v:0", "1800k",
                "-g:v:0", String.valueOf(gop), "-keyint_min:v:0", String.valueOf(gop),
                "-sc_threshold:v:0", "0",
                "-c:a:0", "aac", "-b:a:0", "128k", "-ac:a:0", "2",

                // 720p
                "-map", "[v720out]", "-map", "0:a:0?",
                "-c:v:1", "libx264", "-profile:v:1", "main", "-pix_fmt", "yuv420p",
                "-b:v:1", "2800k", "-maxrate:v:1", "3500k", "-bufsize:v:1", "5000k",
                "-g:v:1", String.valueOf(gop), "-keyint_min:v:1", String.valueOf(gop),
                "-sc_threshold:v:1", "0",
                "-c:a:1", "aac", "-b:a:1", "128k", "-ac:a:1", "2",

                // 1080p
                "-map", "[v1080out]", "-map", "0:a:0?",
                "-c:v:2", "libx264", "-profile:v:2", "main", "-pix_fmt", "yuv420p",
                "-b:v:2", "5500k", "-maxrate:v:2", "6500k", "-bufsize:v:2", "9000k",
                "-g:v:2", String.valueOf(gop), "-keyint_min:v:2", String.valueOf(gop),
                "-sc_threshold:v:2", "0",
                "-c:a:2", "aac", "-b:a:2", "128k", "-ac:a:2", "2",

                // HLS
                "-f", "hls",
                "-hls_time", String.valueOf(segSec),
                "-hls_playlist_type", "vod",
                "-hls_flags", "independent_segments",
                "-hls_segment_type", "mpegts",
                "-master_pl_name", "master.m3u8",
                "-var_stream_map", "v:0,a:0,name:360p v:1,a:1,name:720p v:2,a:2,name:1080p",
                "-hls_segment_filename", segmentPattern,
                playlistPattern
        ));

        processRunner.run(cmd, outputRoot);
    }

    private void createMasterPlaylist(Path outputRoot, List<Rendition> renditions) {
        // TS + aac 포함 codec 문자열은 실제 인코딩 설정에 맞춰 조정 가능
        // 단순화를 위해 고정값 사용(초기 뼈대)
        StringBuilder sb = new StringBuilder();
        sb.append("#EXTM3U\n");
        sb.append("#EXT-X-VERSION:6\n\n");

        for (Rendition r : renditions) {
            // BANDWIDTH는 대략적인 값(비트레이트 기반) - 운영 시 정확히 맞춰도 되고 대략도 OK
            int bandwidth = r.bitrateKbps() * 1000;
            sb.append("#EXT-X-STREAM-INF:")
                    .append("BANDWIDTH=").append(bandwidth).append(",")
                    .append("RESOLUTION=").append(r.width()).append("x").append(r.height()).append(",")
                    .append("CODECS=\"avc1.4d401f,mp4a.40.2\"")
                    .append("\n");
            sb.append(r.height()).append("p/playlist.m3u8\n\n");
        }

        try {
            Files.writeString(outputRoot.resolve("master.m3u8"), sb.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write master.m3u8", e);
        }
    }

    private int calcGop(int segSec, double fps) {
        // round로 세그먼트 경계 근처 맞추기
        int gop = (int) Math.round(segSec * fps);

        // 방어 로직
        if (gop < 1) gop = segSec * 30; // fallback
        // 너무 큰 값 방지(예: fps가 잘못 파싱돼 1000 같은 경우)
        if (gop > 6000) gop = segSec * 30;

        return gop;
    }
}
