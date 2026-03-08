package com.skuri.skuri_backend.infra.notification;

import com.skuri.skuri_backend.domain.notification.service.NotificationDispatchRequest;

import java.util.List;

public interface PushSender {

    PushSendResult send(NotificationDispatchRequest request, List<String> tokens);
}
