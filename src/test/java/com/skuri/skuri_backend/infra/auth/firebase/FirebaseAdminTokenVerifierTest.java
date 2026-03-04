package com.skuri.skuri_backend.infra.auth.firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FirebaseAdminTokenVerifierTest {

    @Mock
    private FirebaseAuth firebaseAuth;

    @Mock
    private FirebaseToken firebaseToken;

    @InjectMocks
    private FirebaseAdminTokenVerifier firebaseAdminTokenVerifier;

    @Test
    void verify_google로그인_claim정상추출() throws Exception {
        when(firebaseAuth.verifyIdToken("valid-token")).thenReturn(firebaseToken);
        when(firebaseToken.getUid()).thenReturn("uid-1");
        when(firebaseToken.getEmail()).thenReturn("user@sungkyul.ac.kr");
        when(firebaseToken.getName()).thenReturn("홍길동");
        when(firebaseToken.getPicture()).thenReturn("https://example.com/profile.jpg");
        when(firebaseToken.getClaims()).thenReturn(
                Map.of(
                        "firebase", Map.of(
                                "sign_in_provider", "google.com",
                                "identities", Map.of("google.com", List.of("google-provider-id"))
                        )
                )
        );

        FirebaseTokenClaims claims = firebaseAdminTokenVerifier.verify("valid-token");

        assertEquals("uid-1", claims.uid());
        assertEquals("user@sungkyul.ac.kr", claims.email());
        assertEquals("google.com", claims.signInProvider());
        assertEquals("google-provider-id", claims.providerId());
        assertEquals("홍길동", claims.providerDisplayName());
        assertEquals("https://example.com/profile.jpg", claims.photoUrl());
    }

    @Test
    void verify_password로그인_identity없어도_인증성공() throws Exception {
        when(firebaseAuth.verifyIdToken("password-token")).thenReturn(firebaseToken);
        when(firebaseToken.getUid()).thenReturn("uid-2");
        when(firebaseToken.getEmail()).thenReturn("admin@sungkyul.ac.kr");
        when(firebaseToken.getName()).thenReturn(null);
        when(firebaseToken.getPicture()).thenReturn(null);
        when(firebaseToken.getClaims()).thenReturn(
                Map.of(
                        "firebase", Map.of(
                                "sign_in_provider", "password"
                        )
                )
        );

        FirebaseTokenClaims claims = firebaseAdminTokenVerifier.verify("password-token");

        assertEquals("password", claims.signInProvider());
        assertNull(claims.providerId());
        assertNull(claims.providerDisplayName());
        assertNull(claims.photoUrl());
    }

    @Test
    void verify_토큰검증실패_InvalidFirebaseTokenException() throws Exception {
        when(firebaseAuth.verifyIdToken("invalid-token")).thenThrow(mock(FirebaseAuthException.class));

        assertThrows(InvalidFirebaseTokenException.class, () -> firebaseAdminTokenVerifier.verify("invalid-token"));
    }
}
