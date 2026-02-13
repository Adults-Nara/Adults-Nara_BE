package com.ott.core.modules.video.dto;

import org.springframework.http.HttpHeaders;

public record PlayResult(
        HttpHeaders httpHeaders,
        String masterUrl,
        long expiresAtEpochSeconds
) {
}
