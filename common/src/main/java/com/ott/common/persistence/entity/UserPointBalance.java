package com.ott.common.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "user_point_balances")
public class UserPointBalance {
    @Id
    private Long userId; // Users.user_id와 동일 (PK이자 FK)

    @Column(nullable = false)
    private int currentBalance = 0; // 현재 잔액

    private OffsetDateTime lastUpdatedAt;
}