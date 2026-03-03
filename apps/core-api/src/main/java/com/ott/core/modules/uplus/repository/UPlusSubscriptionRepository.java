package com.ott.core.modules.uplus.repository;

import com.ott.common.persistence.entity.UPlusSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UPlusSubscriptionRepository extends JpaRepository<UPlusSubscription, Long> {

    Optional<UPlusSubscription> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    /**
     * 스케줄러용: 활성 가입자의 userId 목록만 조회 (메모리 절약)
     */
    @Query("SELECT u.userId FROM UPlusSubscription u WHERE u.active = true")
    List<Long> findActiveUserIds();
}