package com.ott.batch.monthly.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/**
 * tag_stats 테이블에 저장될 집계 데이터.
 * stats_date는 전월 1일 (배치의 집계 기준월 대표값)
 */
@Getter
@Builder
public class TagStatDto {

    private Long userId;
    private Long tagId;
    private String tagName;

    /** 집계 기준 날짜 (전월 1일. 예: 2025-02-01) */
    private LocalDate statsDate;

    /** 해당 태그 총 시청 시간(초) */
    private int totalViewTime;

    /** 해당 태그 시청 횟수 */
    private int viewCount;
}