package com.skuri.skuri_backend.infra.auth.firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@ConditionalOnBean(FirebaseAuth.class)
public class FirebaseAdminTokenVerifier implements FirebaseTokenVerifier {

    private static final String FIREBASE_KEY = "firebase";
    private static final String IDENTITIES_KEY = "identities";
    private static final String GOOGLE_PROVIDER_KEY = "google.com";

    private final FirebaseAuth firebaseAuth;

    @Override
    public FirebaseTokenClaims verify(String idToken) {
        try {
            FirebaseToken token = firebaseAuth.verifyIdToken(idToken);
            return new FirebaseTokenClaims(
                    token.getUid(),
                    token.getEmail(),
                    resolveGoogleProviderId(token),
                    token.getName(),
                    token.getPicture()
            );
        } catch (FirebaseAuthException e) {
            throw new InvalidFirebaseTokenException();
        }
    }

    private String resolveGoogleProviderId(FirebaseToken token) {
        Object firebaseClaim = token.getClaims().get(FIREBASE_KEY);
        if (!(firebaseClaim instanceof Map<?, ?> firebaseMap)) {
            throw new InvalidFirebaseTokenException("provider 식별자(google.com identities)를 찾을 수 없습니다.");
        }

        Object identitiesClaim = firebaseMap.get(IDENTITIES_KEY);
        if (!(identitiesClaim instanceof Map<?, ?> identitiesMap)) {
            throw new InvalidFirebaseTokenException("provider 식별자(google.com identities)를 찾을 수 없습니다.");
        }

        Object googleIdentity = identitiesMap.get(GOOGLE_PROVIDER_KEY);
        if (!(googleIdentity instanceof List<?> list) || list.isEmpty() || !(list.getFirst() instanceof String providerId)) {
            throw new InvalidFirebaseTokenException("provider 식별자(google.com identities)를 찾을 수 없습니다.");
        }

        return providerId;
    }
}
