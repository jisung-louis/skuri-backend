package com.skuri.skuri_backend.infra.auth.firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@ConditionalOnBean(FirebaseAuth.class)
public class FirebaseAdminTokenVerifier implements FirebaseTokenVerifier {

    private static final String FIREBASE_KEY = "firebase";
    private static final String SIGN_IN_PROVIDER_KEY = "sign_in_provider";
    private static final String IDENTITIES_KEY = "identities";

    private final FirebaseAuth firebaseAuth;

    @Override
    public FirebaseTokenClaims verify(String idToken) {
        try {
            FirebaseToken token = firebaseAuth.verifyIdToken(idToken);
            String signInProvider = resolveSignInProvider(token);
            return new FirebaseTokenClaims(
                    token.getUid(),
                    token.getEmail(),
                    signInProvider,
                    resolveProviderId(token, signInProvider),
                    token.getName(),
                    token.getPicture()
            );
        } catch (FirebaseAuthException e) {
            throw new InvalidFirebaseTokenException();
        }
    }

    private String resolveSignInProvider(FirebaseToken token) {
        Object firebaseClaim = token.getClaims().get(FIREBASE_KEY);
        if (!(firebaseClaim instanceof Map<?, ?> firebaseMap)) {
            return null;
        }

        Object signInProviderClaim = firebaseMap.get(SIGN_IN_PROVIDER_KEY);
        if (signInProviderClaim instanceof String signInProvider && StringUtils.hasText(signInProvider)) {
            return signInProvider;
        }
        return null;
    }

    private String resolveProviderId(FirebaseToken token, String signInProvider) {
        if (!StringUtils.hasText(signInProvider)) {
            return null;
        }

        Object firebaseClaim = token.getClaims().get(FIREBASE_KEY);
        if (!(firebaseClaim instanceof Map<?, ?> firebaseMap)) {
            return null;
        }

        Object identitiesClaim = firebaseMap.get(IDENTITIES_KEY);
        if (!(identitiesClaim instanceof Map<?, ?> identitiesMap)) {
            return null;
        }

        Object providerIdentity = identitiesMap.get(signInProvider);
        if (!(providerIdentity instanceof List<?> list)
                || list.isEmpty()
                || !(list.getFirst() instanceof String providerId)
                || !StringUtils.hasText(providerId)) {
            return null;
        }

        return providerId;
    }
}
