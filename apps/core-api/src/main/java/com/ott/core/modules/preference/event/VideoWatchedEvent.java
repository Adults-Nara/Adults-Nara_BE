package com.ott.core.modules.preference.event;

public record VideoWatchedEvent(
        Long userId,
        Long videoId,
        Integer watchSeconds,
        boolean isCompleted
) {
}