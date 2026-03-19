package com.ott.common.outbox.repository;

import com.ott.common.outbox.entity.OutboxEvent;
import com.ott.common.outbox.enums.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus status);

    @Query("SELECT e.id FROM OutboxEvent e WHERE e.status = :status ORDER BY e.createdAt ASC")
    List<Long> findIdsByStatus(@Param("status") OutboxStatus status, Pageable pageable);
}
