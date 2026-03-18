package com.ott.core.modules.backoffice.event;

import java.util.List;

public record VideoVisibilityChangedEvent(List<Long> videoIds, boolean isVisible) {
}
