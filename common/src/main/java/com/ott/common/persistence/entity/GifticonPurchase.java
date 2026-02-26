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
@Table(name = "gifticon_purchase")
public class GifticonPurchase extends BaseEntity {
    @Id
    @Column(name = "gifticon_purchase_id")
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private int pointsUsed; // 사용된 포인트 (예: 3000)

    @Column(length = 500)
    private String gifticonCode; // 발급된 기프티콘 번호(MOCK 처리)

    @Enumerated(EnumType.STRING)
    private PurchaseStatus status;

    public enum PurchaseStatus {
        REQUESTED, COMPLETED, FAILED
    }

    @PrePersist
    private void prePersist() {
        if (id == null) id = IdGenerator.generate();
    }
}