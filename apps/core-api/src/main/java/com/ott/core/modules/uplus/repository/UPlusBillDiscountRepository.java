package com.ott.core.modules.uplus.repository;

import com.ott.common.persistence.entity.UPlusBillDiscount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UPlusBillDiscountRepository extends JpaRepository<UPlusBillDiscount, Long> {

    List<UPlusBillDiscount> findByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByUserIdAndBillingYearMonth(Long userId, String billingYearMonth);
}