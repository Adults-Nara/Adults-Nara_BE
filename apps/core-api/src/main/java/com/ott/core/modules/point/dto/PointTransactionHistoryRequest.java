package com.ott.core.modules.point.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PointTransactionHistoryRequest {
    private OffsetDateTime startDate;
    private OffsetDateTime endDate;
}
