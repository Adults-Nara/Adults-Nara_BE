package com.ott.core.modules.interaction.dto;

import com.ott.common.persistence.enums.InteractionType;

public record InteractionStatusResponseDto(
        String interactionType
) {
    public static InteractionStatusResponseDto from(InteractionType type) {
        return new InteractionStatusResponseDto(type != null ? type.name() : "NONE");
    }
}
