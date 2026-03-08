package com.skuri.skuri_backend.domain.notification.repository.projection;

public interface UnreadCountProjection {

    String getUserId();

    long getUnreadCount();
}
