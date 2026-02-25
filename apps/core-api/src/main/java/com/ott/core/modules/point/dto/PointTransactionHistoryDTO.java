package com.ott.core.modules.point.dto;

import com.ott.common.persistence.entity.PointTransaction;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
public class PointTransactionHistoryDTO {
    private Long transactionId;
    private int amount;
    private PointTransaction.TransactionType type;
    private int balanceAfterTransaction;
    private OffsetDateTime createdAt;

    public static PointTransactionHistoryDTO from(PointTransaction transaction) {
        return PointTransactionHistoryDTO.builder()
                .transactionId(transaction.getId())
                .amount(transaction.getAmount())
                .type(transaction.getType())
                .balanceAfterTransaction(transaction.getBalanceAfterTransaction())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
