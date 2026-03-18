package com.ott.core.modules.usertag.event;

import java.util.List;

public record UserTagsUpdatedEvent(
        Long userId,
        List<String> addedTagNames,
        List<String> removedTagNames
) {
}
