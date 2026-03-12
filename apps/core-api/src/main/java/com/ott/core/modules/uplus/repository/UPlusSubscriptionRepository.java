package com.ott.core.modules.uplus.repository;

import com.ott.common.persistence.entity.UPlusSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UPlusSubscriptionRepository extends JpaRepository<UPlusSubscription, Long> {

    Optional<UPlusSubscription> findByUserId(Long userId);

    Optional<UPlusSubscription> findByPhoneNumber(String phoneNumber);

    @Query("SELECT u.userId FROM UPlusSubscription u WHERE u.active = true")
    List<Long> findActiveUserIds();
}