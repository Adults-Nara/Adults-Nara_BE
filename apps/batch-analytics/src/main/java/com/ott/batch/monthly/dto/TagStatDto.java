package com.ott.batch.monthly.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Step 1: 태그별 통계 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagStatDto {
    private Long tagId;
    private Long userId;
    private LocalDate statsDate;
    private Long totalViewTime;      // 총 시청 시간 (초)
    private Integer viewCount;        // 시청 횟수
    private Integer completedCount;   // 완주 횟수
}