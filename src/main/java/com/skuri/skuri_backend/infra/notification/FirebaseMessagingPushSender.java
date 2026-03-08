package com.skuri.skuri_backend.infra.notification;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import com.skuri.skuri_backend.domain.notification.service.NotificationDispatchRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(FirebaseMessaging.class)
public class FirebaseMessagingPushSender implements PushSender {

    private static final int BATCH_SIZE = 500;

    private final FirebaseMessaging firebaseMessaging;
    private final FirebasePushPayloadMapper payloadMapper;

    @Override
    public PushSendResult send(NotificationDispatchRequest request, List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return PushSendResult.empty();
        }

        List<String> successfulTokens = new ArrayList<>();
        List<String> invalidTokens = new ArrayList<>();

        for (int index = 0; index < tokens.size(); index += BATCH_SIZE) {
            List<String> chunk = tokens.subList(index, Math.min(index + BATCH_SIZE, tokens.size()));

            MulticastMessage message = payloadMapper.toMulticastMessage(request, chunk);

            try {
                BatchResponse response = firebaseMessaging.sendEachForMulticast(message);
                collectResults(response, chunk, successfulTokens, invalidTokens);
            } catch (FirebaseMessagingException e) {
                log.warn("FCM 멀티캐스트 전송 실패: type={}, size={}, message={}", request.type(), chunk.size(), e.getMessage());
            }
        }

        return new PushSendResult(successfulTokens, invalidTokens);
    }

    private void collectResults(
            BatchResponse response,
            List<String> chunk,
            List<String> successfulTokens,
            List<String> invalidTokens
    ) {
        List<SendResponse> responses = response.getResponses();
        for (int index = 0; index < responses.size(); index++) {
            SendResponse sendResponse = responses.get(index);
            String token = chunk.get(index);

            if (sendResponse.isSuccessful()) {
                successfulTokens.add(token);
                continue;
            }

            if (isInvalidToken(sendResponse)) {
                invalidTokens.add(token);
            }
        }
    }

    private boolean isInvalidToken(SendResponse sendResponse) {
        FirebaseMessagingException exception = sendResponse.getException();
        if (exception == null || exception.getMessagingErrorCode() == null) {
            return false;
        }

        MessagingErrorCode errorCode = exception.getMessagingErrorCode();
        return errorCode == MessagingErrorCode.UNREGISTERED || errorCode == MessagingErrorCode.INVALID_ARGUMENT;
    }
}
