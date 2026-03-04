package com.skuri.skuri_backend.infra.auth.firebase;

public record FirebaseTokenClaims(
        String uid,
        String email,
        String signInProvider,
        String providerId,
        String providerDisplayName,
        String photoUrl
) {
}
