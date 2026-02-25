package com.ott.core.modules.point.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
public class UserPointBalanceResponse {
    private int currentBalance;
    private OffsetDateTime lastUpdated;
}
