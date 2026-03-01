package com.ott.core.modules.watch.repository;

import com.ott.common.persistence.entity.WatchHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
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

    @Query("""
            SELECT wh FROM WatchHistory wh
            JOIN FETCH wh.videoMetadata vm
            WHERE wh.user.id = :userId
              AND wh.deleted = false 
              AND vm.deleted = false 
              AND wh.updatedAt >= :threeMonthsAgo
            ORDER BY wh.updatedAt DESC 
            """)
    Slice<WatchHistory> findRecentHistory(@Param("userId") Long userId,
                                          @Param("threeMonthsAgo") OffsetDateTime threeMonthsAgo,
                                          Pageable pageable);

    @Query("SELECT wh FROM WatchHistory wh WHERE wh.user.id = :userId AND wh.videoMetadata.id IN :videoMetadataIds AND wh.deleted = false ")
    List<WatchHistory> findByUserIdAndVideoMetadataIdIn(@Param("userId") Long userId,
                                                        @Param("videoMetadataIds") List<Long> videoMetadataIds);

    @Query(value = """
             SELECT t.tag_id, t.tag_name, SUM(wh.last_position)
             FROM watch_history wh
             JOIN video_tag vt ON wh.video_metadata_id = vt.video_metadata_id
             JOIN tag t ON vt.tag_id = t.tag_id
             WHERE wh.user_id = :userId
               AND wh.deleted = false
             GROUP BY t.tag_id, t.tag_name
             ORDER BY SUM(wh.last_position) DESC 
             LIMIT 8
            """, nativeQuery = true)
    List<Object[]> findTop8TagWatchStatsByUserId(@Param("userId") Long userId);
}
