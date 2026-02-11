package com.ott.media.modules.transcode.service;

import java.nio.file.Path;
import java.util.List;

public interface FfmpegTranscoder {
    void transcodeToHlsTs(Path inputFile, Path outputRoot, int segmentSeconds, List<Integer> renditions);
}
