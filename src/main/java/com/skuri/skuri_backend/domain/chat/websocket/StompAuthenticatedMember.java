package com.skuri.skuri_backend.domain.chat.websocket;

import java.security.Principal;

public record StompAuthenticatedMember(
        String uid,
        String email,
        String signInProvider,
        String providerId,
        String providerDisplayName,
        String photoUrl
) implements Principal {

    @Override
    public String getName() {
        return uid;
    }
}
