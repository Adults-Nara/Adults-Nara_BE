package com.ott.media.modules.transcode.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;

@Component
public class ProcessRunner {
    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(30);
    private static final int TAIL_LINES = 200;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * 기본 타임아웃(30분)으로 실행
     */
    public void run(List<String> command, Path workingDir) {
        run(command, workingDir, DEFAULT_TIMEOUT);
    }

    /**
     * 지정된 타임아웃으로 실행
     */
    public void run(List<String> command, Path workingDir, Duration timeout) {
        if (command == null || command.isEmpty()) {
            throw new IllegalArgumentException("command is empty");
        }
        if (workingDir == null) {
            throw new IllegalArgumentException("workingDir is null");
        }

        // workingDir 존재/디렉토리 보장 (ffmpeg 출력 경로 문제 방지에도 도움)
        try {
            Files.createDirectories(workingDir);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create workingDir: " + workingDir, e);
        }

        // 외부 프로세스를 생성하기 위한 표준 API - 내부적으로 OS 프로세스를 fork/exec
        ProcessBuilder pb = new ProcessBuilder(command);
        // 이 프로세스의 현재 작업 디렉토리 CWD 지정
        pb.directory(workingDir.toFile());
        // stderr -> stdout 합치기
        pb.redirectErrorStream(true);

        Process p = null;
        Deque<String> tail = new ArrayDeque<>(TAIL_LINES);

        try {
            logger.info("Starting process. cwd={}, cmd={}", workingDir, joinCommand(command));
            p = pb.start();

            // 출력은 계속 읽어야 버퍼가 차서 프로세스가 멈추지 않음
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {

                String line;
                while ((line = br.readLine()) != null) {
                    logger.info("[proc] {}", line);

                    // 실패 시 원인 분석용으로 마지막 N줄 보관
                    if (tail.size() == TAIL_LINES) {
                        tail.removeFirst();
                    }
                    tail.addLast(line);
                }
            }

            boolean finished = p.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                // 타임아웃이면 강제 종료
                int pid = safePid(p);
                p.destroy();
                if (!p.waitFor(3, TimeUnit.SECONDS)) {
                    p.destroyForcibly();
                }
                throw new RuntimeException(
                        "Process timeout. pid=" + pid +
                                ", timeout=" + timeout +
                                ", cmd=" + joinCommand(command) +
                                "\n--- last output ---\n" + joinTail(tail)
                );
            }

            int exit = p.exitValue();
            if (exit != 0) {
                throw new RuntimeException(
                        "Process failed. exit=" + exit +
                                ", cmd=" + joinCommand(command) +
                                "\n--- last output ---\n" + joinTail(tail)
                );
            }

            logger.info("Process finished. exit=0, cmd={}", joinCommand(command));

        } catch (InterruptedException e) {
            // 인터럽트는 여기서만 interrupt 상태 복원
            Thread.currentThread().interrupt();

            // 인터럽트 시에도 프로세스 정리
            if (p != null) {
                p.destroy();
                try {
                    if (!p.waitFor(2, TimeUnit.SECONDS)) {
                        p.destroyForcibly();
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }

            throw new RuntimeException(
                    "Process interrupted. cmd=" + joinCommand(command) +
                            "\n--- last output ---\n" + joinTail(tail),
                    e
            );

        } catch (IOException e) {
            // IOException은 interrupt 걸면 안 됨 (지금 코드가 이 부분이 위험했음)
            throw new RuntimeException(
                    "Process execution error. cmd=" + joinCommand(command) +
                            ", cwd=" + workingDir +
                            "\n--- last output ---\n" + joinTail(tail),
                    e
            );
        }
    }

    private static String joinCommand(List<String> command) {
        StringJoiner sj = new StringJoiner(" ");
        for (String c : command) {
            // 공백 포함 인자 로깅 가독성
            if (c.contains(" ")) sj.add("\"" + c + "\"");
            else sj.add(c);
        }
        return sj.toString();
    }

    private static String joinTail(Deque<String> tail) {
        if (tail == null || tail.isEmpty()) return "(no output)";
        StringBuilder sb = new StringBuilder();
        for (String line : tail) {
            sb.append(line).append('\n');
        }
        return sb.toString();
    }

    private static int safePid(Process p) {
        try {
            return (int) p.pid();
        } catch (Throwable t) {
            return -1;
        }
    }
}
