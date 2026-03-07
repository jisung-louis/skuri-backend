package com.skuri.skuri_backend.domain.notification.service;

import com.skuri.skuri_backend.domain.notification.entity.NotificationType;
import com.skuri.skuri_backend.domain.notification.model.NotificationData;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public record NotificationDispatchRequest(
        NotificationType type,
        Set<String> recipientIds,
        String title,
        String message,
        NotificationData data,
        boolean pushEnabled,
        boolean inboxEnabled
) {

    public NotificationDispatchRequest {
        recipientIds = recipientIds == null ? Set.of() : Set.copyOf(recipientIds);
        data = data == null ? NotificationData.empty() : data;
    }

    public static NotificationDispatchRequest of(
            NotificationType type,
            Collection<String> recipientIds,
            String title,
            String message,
            NotificationData data,
            boolean pushEnabled,
            boolean inboxEnabled
    ) {
        return new NotificationDispatchRequest(
                type,
                recipientIds == null ? Set.of() : new LinkedHashSet<>(recipientIds),
                title,
                message,
                data,
                pushEnabled,
                inboxEnabled
        );
    }
}
