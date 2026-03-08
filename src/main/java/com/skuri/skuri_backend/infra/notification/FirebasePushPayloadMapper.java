package com.skuri.skuri_backend.infra.notification;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.skuri.skuri_backend.domain.notification.model.PushPresentationProfile;
import com.skuri.skuri_backend.domain.notification.service.NotificationDispatchRequest;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class FirebasePushPayloadMapper {

    private static final String CONTRACT_VERSION = "1";

    public MulticastMessage toMulticastMessage(NotificationDispatchRequest request, List<String> tokens) {
        MulticastMessage.Builder builder = MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(Notification.builder()
                        .setTitle(request.title())
                        .setBody(request.message())
                        .build())
                .putAllData(buildPushData(request))
                .setAndroidConfig(buildAndroidConfig(request.presentationProfile()))
                .setApnsConfig(buildApnsConfig(request.presentationProfile()));

        return builder.build();
    }

    private Map<String, String> buildPushData(NotificationDispatchRequest request) {
        Map<String, String> pushData = new LinkedHashMap<>();
        pushData.put("contractVersion", CONTRACT_VERSION);
        pushData.put("type", request.type().name());
        pushData.putAll(request.data().toPushData());
        return pushData;
    }

    private AndroidConfig buildAndroidConfig(PushPresentationProfile profile) {
        AndroidConfig.Builder builder = AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH);

        if (profile.hasAndroidNotificationOverrides()) {
            builder.setNotification(AndroidNotification.builder()
                    .setChannelId(profile.androidChannelId())
                    .setSound(profile.androidSound())
                    .build());
        }

        return builder.build();
    }

    private ApnsConfig buildApnsConfig(PushPresentationProfile profile) {
        return ApnsConfig.builder()
                .setAps(Aps.builder()
                        .setSound(profile.iosSound())
                        .build())
                .build();
    }
}
