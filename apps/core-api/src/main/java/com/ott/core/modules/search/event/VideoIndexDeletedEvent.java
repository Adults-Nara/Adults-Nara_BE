package com.ott.core.modules.search.event;

import java.util.List;

public record VideoIndexDeletedEvent(List<Long> videoIds) {
}
