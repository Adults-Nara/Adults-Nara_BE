package com.ott.core.modules.watch.repository;

import com.ott.common.persistence.entity.WatchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface WatchHistoryRepository extends JpaRepository<WatchHistory, Long> {
    Optional<WatchHistory> findByUserIdAndVideoMetadataId(Long userId, Long videoMetadataId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """                                                                                                                               
            INSERT INTO watch_history (watch_history_id, user_id, video_metadata_id, last_position, completed, deleted, created_at, updated_at)          
            VALUES (:id, :userId, :videoMetadataId, :lastPosition, :completed, false, :now, :now)
            ON CONFLICT (user_id, video_metadata_id)
            DO UPDATE SET 
                        last_position = :lastPosition,
                        completed = CASE
                                    WHEN watch_history.completed = true THEN true
                                    ELSE :completed
                                    END,
                        updated_at = :now
            """, nativeQuery = true)
    void upsertWatchHistory(@Param("id") Long id,
                            @Param("userId") Long userId,
                            @Param("videoMetadataId") Long videoMetadataId,
                            @Param("lastPosition") Integer lastPosition,
                            @Param("completed") boolean completed,
                            @Param("now") OffsetDateTime now);


}
