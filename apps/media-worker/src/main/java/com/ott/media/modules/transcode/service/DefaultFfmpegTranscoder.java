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

        // 렌디션별 설정(출발점) - 운영하면서 콘텐츠에 맞게 조정
        // bitrate는 예시값
        Map<Integer, Rendition> ladder = Map.of(
                360, new Rendition(640, 360,  900, 1100, 1800),
                720, new Rendition(1280,720, 2800,3500, 5000),
                1080, new Rendition(1920,1080,5500,6500, 9000)
        );

        // 필요한 화질만 만들기
        List<Rendition> targets = new ArrayList<>();
        for (Integer r : renditions) {
            Rendition rendition = ladder.get(r);
            if (rendition == null) {
                throw new IllegalArgumentException("Unsupported rendition: " + r);
            }
            targets.add(rendition);
        }

        // 단순/안정: 화질별 FFmpeg 개별 실행(처음엔 이게 운영이 편함)
        // 추후 성능 최적화로 1회 실행 멀티출력으로 바꿔도 됨.
        for (Rendition rendition : targets) {
            Path variantDir = outputRoot.resolve(rendition.height() + "p");
            runSingleRendition(inputFile, variantDir, rendition, segmentSeconds, fps);
        }

        // master.m3u8 생성(직접 생성)
        createMasterPlaylist(outputRoot, targets);
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
