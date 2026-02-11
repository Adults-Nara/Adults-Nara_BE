package com.ott.common.persistence.enums;

public enum BanStatus {
    ACTIVE,
    DEACTIVATED,
    SUSPENDED_7,
    SUSPENDED_15,
    SUSPENDED_30,
    PERMANENTLY_BANNED,
    DELETED;

    public boolean isSuspended() {
        return this == SUSPENDED_7 || this == SUSPENDED_15 || this == SUSPENDED_30;
    }

    public int getDays() {
        return switch (this) {
            case SUSPENDED_7 -> 7;
            case SUSPENDED_15 -> 15;
            case SUSPENDED_30 -> 30;
            default -> 0;
        };
    }
}