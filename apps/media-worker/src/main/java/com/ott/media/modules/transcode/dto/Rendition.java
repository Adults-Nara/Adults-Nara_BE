package com.ott.media.modules.transcode.dto;

public record Rendition(
        int width,
        int height,
        int bitrateKbps,
        int maxrateKbps,
        int bufsizeKbps
) {
}
