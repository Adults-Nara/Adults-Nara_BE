package com.ott.common.persistence.entity;

import com.ott.common.persistence.base.BaseEntity;
import com.ott.common.persistence.enums.UPlusSubscriptionPlan;
import com.ott.common.util.IdGenerator;
import jakarta.persistence.*;
import lombok.*;

/**
 * U+ 가입 정보
 *
 * 역할: "이 유저가 U+ 가입자인가?"를 판별하는 테이블.
 *
 * - active=true  → 매월 1일 스케줄러가 보유 포인트를 청구서 할인으로 자동 반영
 * - active=false → 포인트가 쌓여도 U+ 요금 할인 혜택 없음
 *
 * 사용자 1명 : 가입 정보 1개 (1:1)
 */
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(
        name = "uplus_subscription",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_uplus_subscription_user",  columnNames = "user_id"),
                @UniqueConstraint(name = "uk_uplus_subscription_phone", columnNames = "phone_number")
        }
)
public class UPlusSubscription extends BaseEntity {

    @Id
    @Column(name = "uplus_subscription_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * U+ 가입 휴대폰 번호 (하이픈 제거 후 숫자만 저장)
     * ex) "01012345678"
     */
    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    /**
     * 현재 가입 중인 요금제
     * 할인 상한(monthlyFee) 계산에 사용됨
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false, length = 50)
    private UPlusSubscriptionPlan plan;

    /**
     * 가입 활성 여부
     * false(해지) 이면 스케줄러가 이 유저를 건너뜀
     */
    @Column(name = "active", nullable = false)
    private boolean active;

    @PrePersist
    private void prePersist() {
        if (id == null) id = IdGenerator.generate();
    }

    // ===== 도메인 메서드 =====

    public void changePlan(UPlusSubscriptionPlan newPlan) {
        this.plan = newPlan;
    }

    public void deactivate() {
        this.active = false;
    }

    public void reactivate(UPlusSubscriptionPlan newPlan) {
        this.plan = newPlan;
        this.active = true;
    }
}