package com.ott.core.modules.point.repository;

import com.ott.common.persistence.entity.PointTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;


public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {
    // 오늘 자정 이후로 해당 유저가 받은 광고 보상 횟수를 카운트
    int countByUserIdAndTypeAndCreatedAtAfter(Long userId, PointTransaction.TransactionType type, OffsetDateTime startOfDay);
}
