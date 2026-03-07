package com.skuri.skuri_backend.domain.support.entity;

public enum ReportStatus {
    PENDING,
    REVIEWING,
    ACTIONED,
    REJECTED;

    public boolean canTransitionTo(ReportStatus nextStatus) {
        if (this == nextStatus) {
            return true;
        }
        return switch (this) {
            case PENDING -> nextStatus == REVIEWING || nextStatus == ACTIONED || nextStatus == REJECTED;
            case REVIEWING -> nextStatus == ACTIONED || nextStatus == REJECTED;
            case ACTIONED, REJECTED -> false;
        };
    }
}
