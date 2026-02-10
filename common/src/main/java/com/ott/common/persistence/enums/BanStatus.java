package com.ott.common.persistence.enums;

public enum BanStatus {
    ACTIVE("활성화", 0),
    DEACTIVATED("비활성화", 0),
    SUSPENDED_7("7일 정지", 7),
    SUSPENDED_15("15일 정지", 15),
    SUSPENDED_30("30일 정지", 30),
    PERMANENTLY_BANNED("영구 정지", -1),
    DELETED("삭제됨", 0);

    private final String description;
    private final int days;

    BanStatus(String description, int days) {
        this.description = description;
        this.days = days;
    }

    public String getDescription() {
        return description;
    }

    public int getDays() {
        return days;
    }

    public boolean isSuspended() {
        return this == SUSPENDED_7 || this == SUSPENDED_15 || this == SUSPENDED_30;
    }

    public boolean isDeleted() {
        return this == DELETED;
    }
}