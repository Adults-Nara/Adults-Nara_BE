package com.ott.core.modules.point.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PointTransactionHistoryRequest {
    @Schema(description = "조회 시작일", example = "2026-02-01", type = "string")
    private String startDate; // "yyyy-MM-dd" 형식
    @Schema(description = "조회 종료일", example = "2026-02-25", type = "string")
    private String endDate;
}
