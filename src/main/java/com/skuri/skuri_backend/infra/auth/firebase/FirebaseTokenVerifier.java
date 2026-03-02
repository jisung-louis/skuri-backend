package com.skuri.skuri_backend.infra.auth.firebase;

public interface FirebaseTokenVerifier {

    FirebaseTokenClaims verify(String idToken);
}
