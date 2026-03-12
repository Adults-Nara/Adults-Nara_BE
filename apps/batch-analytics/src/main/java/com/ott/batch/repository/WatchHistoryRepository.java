package com.ott.batch.repository;

import com.ott.common.persistence.entity.WatchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface WatchHistoryRepository extends JpaRepository<WatchHistory, Long> {

    @Query("""
        SELECT wh FROM WatchHistory wh
        JOIN wh.user u
        WHERE u.id = :userId
          AND wh.createdAt >= :rangeFrom 
          AND wh.createdAt < :rangeTo
    """)
    List<WatchHistory> findByUserIdAndCreatedAtBetween(
            @Param("userId") Long userId,
            @Param("rangeFrom") OffsetDateTime rangeFrom,
            @Param("rangeTo") OffsetDateTime rangeTo
    );
}