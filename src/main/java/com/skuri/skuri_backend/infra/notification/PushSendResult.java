package com.skuri.skuri_backend.infra.notification;

import java.util.List;

public record PushSendResult(
        List<String> successfulTokens,
        List<String> invalidTokens
) {

    public static PushSendResult empty() {
        return new PushSendResult(List.of(), List.of());
    }
}
