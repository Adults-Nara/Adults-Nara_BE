package com.ott.batch.repository;

import com.ott.common.persistence.entity.WatchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface WatchHistoryRepository extends JpaRepository<WatchHistory, Long> {

    /**
     * 사용자 ID와 생성 시간 범위로 시청 기록 조회 (시간대별 집계용)
     */
    List<WatchHistory> findByUserIdAndCreatedAtBetween(
            Long userId,
            OffsetDateTime startDateTime,
            OffsetDateTime endDateTime
    );

    @Transactional
    @Modifying
    @Query(value = """
        INSERT INTO watch_history (watch_history_id, user_id, video_metadata_id, last_position, completed, created_at)
        VALUES (:watchHistoryId, :userId, :videoMetadataId, :lastPosition, :completed, :createdAt)
        ON CONFLICT (watch_history_id) DO UPDATE SET
            last_position = EXCLUDED.last_position,
            completed = EXCLUDED.completed,
            updated_at = CURRENT_TIMESTAMP
        """, nativeQuery = true)
    void upsertWatchHistory(
            @Param("watchHistoryId") Long watchHistoryId,
            @Param("userId") Long userId,
            @Param("videoMetadataId") Long videoMetadataId,
            @Param("lastPosition") Integer lastPosition,
            @Param("completed") Boolean completed,
            @Param("createdAt") OffsetDateTime createdAt
    );
}