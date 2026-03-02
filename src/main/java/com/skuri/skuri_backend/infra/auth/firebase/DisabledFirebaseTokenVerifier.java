package com.skuri.skuri_backend.infra.auth.firebase;

import com.google.firebase.auth.FirebaseAuth;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(FirebaseAuth.class)
public class DisabledFirebaseTokenVerifier implements FirebaseTokenVerifier {

    @Override
    public FirebaseTokenClaims verify(String idToken) {
        throw new InvalidFirebaseTokenException("Firebase 인증 인프라가 설정되지 않았습니다.");
    }
}
