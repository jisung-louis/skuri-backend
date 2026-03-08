package com.skuri.skuri_backend.domain.notification.service;

import com.skuri.skuri_backend.domain.notification.repository.FcmTokenRepository;
import com.skuri.skuri_backend.infra.notification.PushSendResult;
import com.skuri.skuri_backend.infra.notification.PushSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PushNotificationService {

    private final FcmTokenRepository fcmTokenRepository;
    private final FcmTokenService fcmTokenService;
    private final PushSender pushSender;

    @Transactional
    public void send(NotificationDispatchRequest request) {
        if (!request.pushEnabled() || request.recipientIds().isEmpty()) {
            return;
        }

        List<String> tokens = fcmTokenRepository.findByUserIdIn(request.recipientIds()).stream()
                .map(fcmToken -> fcmToken.getToken())
                .distinct()
                .toList();

        if (tokens.isEmpty()) {
            return;
        }

        PushSendResult result = pushSender.send(request, tokens);
        fcmTokenService.touchTokens(result.successfulTokens());
        fcmTokenService.purgeTokens(result.invalidTokens());
    }
}
