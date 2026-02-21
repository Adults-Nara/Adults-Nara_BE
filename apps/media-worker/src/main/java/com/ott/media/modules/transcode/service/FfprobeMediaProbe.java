package com.ott.media.modules.transcode.service;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class FfprobeMediaProbe {
    public double readFps(Path inputFile) {
        // avg_frame_rate를 한 줄로 출력: 예) "30000/1001"
        List<String> cmd = List.of(
                "ffprobe",
                "-v", "error",
                "-select_streams", "v:0",
                "-show_entries", "stream=avg_frame_rate",
                "-of", "default=nw=1:nk=1",
                inputFile.toString()
        );

        String out = runAndCapture(cmd, inputFile.getParent());
        double fps = parseFps(out);

        // 비정상 값 방어
        if (fps <= 0.1 || Double.isNaN(fps) || Double.isInfinite(fps)) {
            // fallback: 30fps
            return 30.0;
        }
        return fps;
    }

    public double readDurationSeconds(Path inputFile) {
        List<String> cmd = List.of(
                "ffprobe",
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                inputFile.toString()
        );

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);

        try {
            Process p = pb.start();

            String output;
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                output = br.readLine();
            }

            int exit = p.waitFor();
            if (exit != 0) {
                throw new RuntimeException("ffprobe failed. exit=" + exit);
            }

            if (output == null || output.isBlank()) {
                throw new RuntimeException("ffprobe returned empty duration");
            }

            return Double.parseDouble(output.trim());

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("ffprobe execution error", e);
        }
    }

    private String runAndCapture(List<String> cmd, Path workingDir) {
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            if (workingDir != null) pb.directory(workingDir.toFile());
            pb.redirectErrorStream(true);

            Process p = pb.start();

            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }

            boolean finished = p.waitFor(15, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                throw new RuntimeException("ffprobe timeout");
            }

            int exit = p.exitValue();
            if (exit != 0) {
                throw new RuntimeException("ffprobe failed. exit=" + exit + ", cmd=" + String.join(" ", cmd));
            }

            return sb.toString().trim();
        } catch (Exception e) {
            throw new RuntimeException("ffprobe execution error", e);
        }
    }

    private double parseFps(String s) {
        if (s == null) return -1;
        String t = s.trim();
        if (t.isEmpty()) return -1;

        // 예: "30000/1001"
        if (t.contains("/")) {
            String[] parts = t.split("/");
            if (parts.length != 2) return -1;
            try {
                double num = Double.parseDouble(parts[0]);
                double den = Double.parseDouble(parts[1]);
                if (den == 0) return -1;
                return num / den;
            } catch (NumberFormatException e) {
                return -1;
            }
        }

        // 예: "29.97"
        try {
            return Double.parseDouble(t);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
