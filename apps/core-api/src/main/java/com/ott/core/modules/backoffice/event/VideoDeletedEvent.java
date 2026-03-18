package com.ott.core.modules.backoffice.event;

import java.util.List;

public record VideoDeletedEvent(List<Long> videoIds) {
}
