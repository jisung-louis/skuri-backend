package com.skuri.skuri_backend.infra.auth.config;

import com.google.auth.oauth2.GoogleCredentials;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class FirebaseConfigTest {

    @Test
    void emulator사용시_잘못된경로가있어도_에뮬레이터자격증명을우선사용한다() {
        FirebaseConfig firebaseConfig = new FirebaseConfig();

        GoogleCredentials credentials = ReflectionTestUtils.invokeMethod(
                firebaseConfig,
                "loadCredentials",
                "/app/secrets/firebase-admin.json",
                true
        );

        assertNotNull(credentials);
    }
}
