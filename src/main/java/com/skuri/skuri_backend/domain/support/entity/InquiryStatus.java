package com.skuri.skuri_backend.domain.support.entity;

public enum InquiryStatus {
    PENDING,
    IN_PROGRESS,
    RESOLVED;

    public boolean canTransitionTo(InquiryStatus nextStatus) {
        if (this == nextStatus) {
            return true;
        }
        return switch (this) {
            case PENDING -> nextStatus == IN_PROGRESS || nextStatus == RESOLVED;
            case IN_PROGRESS -> nextStatus == RESOLVED;
            case RESOLVED -> false;
        };
    }
}
