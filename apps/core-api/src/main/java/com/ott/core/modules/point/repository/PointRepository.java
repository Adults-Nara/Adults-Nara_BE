package com.ott.core.modules.point.repository;

import com.ott.common.persistence.entity.UserPointBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PointRepository extends JpaRepository<UserPointBalance, Long> {
    @Query("SELECT COALESCE(u.currentBalance, 0) FROM UserPointBalance u WHERE u.userId = :userId")
    int findUserPointBalanceByUserId(@Param("userId") Long userId);
}
