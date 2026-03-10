package com.ott.batch.monthly.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자별 태그 시청 원본 데이터 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserTagWatchRaw {

    private Long userId;
    private Long tagId;
    private String tagName;

    // 날짜 범위 내 통계
    private long totalViewTime;  // int → long 변경
    private int viewCount;
    private int completedCount;
}