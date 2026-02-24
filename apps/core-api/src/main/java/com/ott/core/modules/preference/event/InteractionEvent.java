package com.ott.core.modules.preference.event;

import com.ott.common.persistence.enums.InteractionType;

public record InteractionEvent(
        Long userId,
        Long videoId,
        InteractionType oldType, // 이전 상태 (새로 누른 거면 null)
        InteractionType newType  // 새로운 상태 (취소한 거면 null)
) {
}