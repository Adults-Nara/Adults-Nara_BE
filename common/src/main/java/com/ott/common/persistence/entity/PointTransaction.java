package com.ott.common.persistence.entity;

import com.ott.common.persistence.base.BaseEntity;
import com.ott.common.util.IdGenerator;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "point_transactions", indexes = {
        @Index(name = "idx_point_user_created", columnList = "userId, createdAt"),
        @Index(name = "idx_point_transaction_key", columnList = "transactionKey", unique = true),
        @Index(name = "idx_point_user_type_date", columnList = "userId, type, createdAt")
})
public class PointTransaction extends BaseEntity {
    @Id
    @Column(name = "point_transaction_id")
    private Long id;

    @Column(nullable = false)
    private Long userId;

    // 중복 적립 방지용 고유 키 (예: "AD_REWARD_20260224_user123_video456")
    @Column(nullable = false, unique = true)
    private String transactionKey;

    @Column(nullable = false)
    private int amount; // 적립(+) 또는 사용(-) 금액

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    // 광고 시청인 경우 videoMetadataId, 구매인 경우 gifticonPurchaseId 등을 저장하여 추적성 확보
    private Long referenceId;

    @Column(nullable = false)
    private int balanceAfterTransaction; // 거래 후 잔액 스냅샷

    public enum TransactionType {
        AD_REWARD, //광고 시청 보상
        PURCHASE_BONUS, //구매 보상
        GIFTICON_PURCHASE, //기프티콘 교환
        UPLUS_DISCOUNT, // U+ 요금제 자동 할인 차감
    }

    @PrePersist
    private void prePersist() {
        if (id == null) id = IdGenerator.generate();
    }
}