package com.ott.core.modules.backoffice.event;

public record VideoUpdatedEvent(Long videoId, boolean isVisible) {
}
