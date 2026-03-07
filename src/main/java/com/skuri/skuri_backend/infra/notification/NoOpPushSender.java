package com.skuri.skuri_backend.infra.notification;

import com.skuri.skuri_backend.domain.notification.service.NotificationDispatchRequest;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnMissingBean(FirebaseMessaging.class)
public class NoOpPushSender implements PushSender {

    @Override
    public PushSendResult send(NotificationDispatchRequest request, List<String> tokens) {
        return PushSendResult.empty();
    }
}
