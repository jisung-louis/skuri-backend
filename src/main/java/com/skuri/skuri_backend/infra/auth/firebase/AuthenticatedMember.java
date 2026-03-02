package com.skuri.skuri_backend.infra.auth.firebase;

public record AuthenticatedMember(
        String uid,
        String email,
        String providerId,
        String providerDisplayName,
        String photoUrl
) {

    public static AuthenticatedMember from(FirebaseTokenClaims claims) {
        return new AuthenticatedMember(
                claims.uid(),
                claims.email(),
                claims.providerId(),
                claims.providerDisplayName(),
                claims.photoUrl()
        );
    }
}
