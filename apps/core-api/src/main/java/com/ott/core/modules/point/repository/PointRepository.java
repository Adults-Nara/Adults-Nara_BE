package com.ott.core.modules.point.repository;

import com.ott.common.persistence.entity.UserPointBalance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointRepository extends JpaRepository<UserPointBalance, Long> {
    int findUserPointBalanceByUserId(Long userId);
}
