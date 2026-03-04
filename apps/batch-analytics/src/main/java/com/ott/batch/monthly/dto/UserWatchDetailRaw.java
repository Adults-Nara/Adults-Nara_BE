package com.ott.batch.monthly.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Step2 Reader가 DB에서 읽어오는 사용자별 시청 raw 데이터.
 *
 * watch_history.updated_at 기준으로 시간대 분류 및 완주 여부를 포함한다.
 *
 * 쿼리:
 *   SELECT wh.user_id,
 *          wh.last_position,
 *          wh.completed,
 *          EXTRACT(HOUR FROM wh.updated_at AT TIME ZONE 'Asia/Seoul') as watch_hour,
 *          t.tag_name,
 *          tp.tag_name as parent_tag_name  -- 다양성 점수용 부모 태그
 *   FROM watch_history wh
 *   JOIN video_tag vt ON wh.video_metadata_id = vt.video_metadata_id
 *   JOIN tag t ON vt.tag_id = t.tag_id
 *   LEFT JOIN tag tp ON t.parent_id = tp.tag_id
 *   WHERE wh.updated_at BETWEEN :from AND :to
 *     AND wh.deleted = false
 */
@Getter
@AllArgsConstructor
public class UserWatchDetailRaw {
    private Long userId;
    private int lastPosition;
    private boolean completed;
    private int watchHour;           // 0~23 (KST)
    private String tagName;
    private String parentTagName;    // null이면 최상위 태그
}