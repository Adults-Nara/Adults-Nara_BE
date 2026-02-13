package com.ott.core.modules.video.controller.response;

import com.ott.core.modules.video.dto.PlayResult;

public record PlayResponse(
        String masterUrl,
        long expiresAtEpochSeconds
) {
    public static PlayResponse of(PlayResult result) {
        return new PlayResponse(result.masterUrl(), result.expiresAtEpochSeconds());
    }
}
