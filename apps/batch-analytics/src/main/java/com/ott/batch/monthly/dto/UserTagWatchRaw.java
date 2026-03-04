package com.ott.batch.monthly.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Step1 Reader가 DB에서 읽어오는 사용자별 태그 집계 raw 데이터.
 *
 * 쿼리:
 *   SELECT wh.user_id, vt.tag_id, t.tag_name,
 *          SUM(wh.last_position) as total_view_time,
 *          COUNT(*) as view_count
 *   FROM watch_history wh
 *   JOIN video_tag vt ON wh.video_metadata_id = vt.video_metadata_id
 *   JOIN tag t ON vt.tag_id = t.tag_id
 *   WHERE wh.updated_at BETWEEN :from AND :to
 *     AND wh.deleted = false
 *   GROUP BY wh.user_id, vt.tag_id, t.tag_name
 */
@Getter
@AllArgsConstructor
public class UserTagWatchRaw {
    private Long userId;
    private Long tagId;
    private String tagName;
    private int totalViewTime;
    private int viewCount;
}