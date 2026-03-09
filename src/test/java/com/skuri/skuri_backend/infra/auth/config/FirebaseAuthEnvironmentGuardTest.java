package com.skuri.skuri_backend.infra.auth.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FirebaseAuthEnvironmentGuardTest {

    @Mock
    private Environment environment;

    private FirebaseAuthProperties properties;
    private FirebaseAuthEnvironmentGuard guard;

    @BeforeEach
    void setUp() {
        properties = new FirebaseAuthProperties();
        guard = new FirebaseAuthEnvironmentGuard(environment, properties);
    }

    @Test
    void localEmulator프로필_필수값충족_성공() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"local-emulator"});
        properties.setUseEmulator(true);
        properties.setEmulatorHost("127.0.0.1:9099");
        properties.setProjectId("sktaxi-acb4c");

        assertDoesNotThrow(() -> guard.validate());
    }

    @Test
    void emulator사용인데_localEmulator프로필아님_실패() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"local"});
        properties.setUseEmulator(true);
        properties.setEmulatorHost("127.0.0.1:9099");
        properties.setProjectId("sktaxi-acb4c");

        assertThrows(IllegalStateException.class, () -> guard.validate());
    }

    @Test
    void emulator사용인데_host없음_실패() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"local-emulator"});
        properties.setUseEmulator(true);
        properties.setEmulatorHost("");
        properties.setProjectId("sktaxi-acb4c");

        assertThrows(IllegalStateException.class, () -> guard.validate());
    }

    @Test
    void localEmulator프로필인데_emulator비활성화_실패메시지_명확() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"local-emulator"});
        when(environment.getProperty("GOOGLE_APPLICATION_CREDENTIALS"))
                .thenReturn("/app/secrets/firebase-admin.json");
        properties.setUseEmulator(false);
        properties.setCredentialsPath("/app/secrets/firebase-admin.json");
        properties.setProjectId("sktaxi-acb4c");

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> guard.validate());

        assertTrue(exception.getMessage().contains("local-emulator 프로필에서는 firebase.auth.use-emulator=true 이어야 합니다."));
        assertTrue(exception.getMessage().contains("IntelliJ 환경 변수 칸에는 .env 파일 경로가 아니라 KEY=value 형식으로 직접 입력해야 합니다."));
    }

    @Test
    void emulator미사용인데_host설정됨_실패() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"local"});
        properties.setUseEmulator(false);
        properties.setEmulatorHost("127.0.0.1:9099");

        assertThrows(IllegalStateException.class, () -> guard.validate());
    }
}
