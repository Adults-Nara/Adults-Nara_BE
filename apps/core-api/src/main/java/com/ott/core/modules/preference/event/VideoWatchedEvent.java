package com.ott.core.modules.preference.event;

public record VideoWatchedEvent(
        Long userId,
        Long videoMetadataId,
        Integer watchSeconds,
        boolean isCompleted
) {
}