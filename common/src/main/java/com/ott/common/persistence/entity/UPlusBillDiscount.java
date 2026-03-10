package com.ott.common.persistence.entity;

import com.ott.common.persistence.base.BaseEntity;
import com.ott.common.persistence.enums.UPlusSubscriptionPlan;
import com.ott.common.util.IdGenerator;
import jakarta.persistence.*;
import lombok.*;

/**
 * U+ 포인트 자동 할인 이력
 *
 * 스케줄러(UPlusDiscountScheduler)가 매월 1일에 가입자당 1건 생성.
 *
 * 할인 산출:
 *   discountAmount = min(현재 보유 포인트, 요금제.monthlyFee)
 */
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(
        name = "uplus_bill_discount",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_uplus_bill_discount_user_month",
                        columnNames = {"user_id", "billing_year_month"}
                )
        },
        indexes = {
                @Index(name = "idx_uplus_bill_discount_user", columnList = "user_id")
        }
)
public class UPlusBillDiscount extends BaseEntity {

    @Id
    @Column(name = "uplus_bill_discount_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 할인이 적용되는 청구 연월 (YYYY-MM)
     * ex) "2026-03" → 3월 U+ 청구서에서 discountAmount 만큼 할인
     */
    @Column(name = "billing_year_month", nullable = false, length = 7)
    private String billingYearMonth;

    /**
     * 처리 시점의 요금제 스냅샷 (변경 가능하므로 이력에 고정)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false, length = 50)
    private UPlusSubscriptionPlan plan;

    /**
     * 실제 차감된 포인트 수 = 할인 금액(원)
     * 포인트가 0이면 0으로 기록
     */
    @Column(name = "discount_amount", nullable = false)
    private int discountAmount;

    @PrePersist
    private void prePersist() {
        if (id == null) id = IdGenerator.generate();
    }
}