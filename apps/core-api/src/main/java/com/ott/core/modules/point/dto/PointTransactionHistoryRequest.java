package com.ott.core.modules.point.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PointTransactionHistoryRequest {
    private String startDate; // "yyyy-MM-dd" 형식
    private String endDate;
}
