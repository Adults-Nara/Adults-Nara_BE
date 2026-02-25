package com.ott.core.modules.point.repository;

import com.ott.common.persistence.entity.UserPointBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface PointRepository extends JpaRepository<UserPointBalance, Long> {
    UserPointBalance findUserPointBalanceByUserId(@Param("userId") Long userId);
}
