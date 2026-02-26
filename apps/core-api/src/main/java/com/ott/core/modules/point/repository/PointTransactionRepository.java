package com.ott.core.modules.point.repository;

import com.ott.common.persistence.entity.PointTransaction;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;

import java.util.List;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {
        // 오늘 자정 이후로 해당 유저가 받은 광고 보상 횟수를 카운트
        int countByUserIdAndTypeAndCreatedAtAfter(Long userId, PointTransaction.TransactionType type,
                        OffsetDateTime startOfDay);

        List<PointTransaction> findAllByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(Long userId,
                        OffsetDateTime startOfDay, OffsetDateTime endOfDay);

        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Query("UPDATE UserPointBalance b SET b.currentBalance = :newBalance, b.lastUpdatedAt = :newUpdatedAt WHERE b.userId = :userId")
        void updateUserPoint(@Param("userId") Long userId,
                             @Param("newBalance") int newBalance,
                             @Param("newUpdatedAt") OffsetDateTime newUpdatedAt);
}
