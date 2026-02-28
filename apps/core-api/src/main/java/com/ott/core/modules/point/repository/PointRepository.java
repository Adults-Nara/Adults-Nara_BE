package com.ott.core.modules.point.repository;

import com.ott.common.persistence.entity.UserPointBalance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PointRepository extends JpaRepository<UserPointBalance, Long> {
    UserPointBalance findUserPointBalanceByUserId(@Param("userId") Long userId);

    //비관적 락
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from UserPointBalance p where p.userId = :userId")
    UserPointBalance findUserPointBalanceByUserIdUpdateLock(@Param("userId")  Long userId);
}
