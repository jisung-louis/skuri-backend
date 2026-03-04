package com.skuri.skuri_backend.infra.auth.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class FirebaseAuthEnvironmentGuard {

    private static final String LOCAL_EMULATOR_PROFILE = "local-emulator";

    private final Environment environment;
    private final FirebaseAuthProperties properties;

    @PostConstruct
    void validate() {
        boolean localEmulatorProfileActive = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(LOCAL_EMULATOR_PROFILE::equals);
        boolean useEmulator = properties.isUseEmulator();
        boolean emulatorHostConfigured = StringUtils.hasText(properties.getEmulatorHost());

        if (useEmulator) {
            if (!localEmulatorProfileActive) {
                throw new IllegalStateException(
                        "firebase.auth.use-emulator=true 는 local-emulator 프로필에서만 허용됩니다."
                );
            }
            if (!emulatorHostConfigured) {
                throw new IllegalStateException(
                        "local-emulator 프로필에서는 FIREBASE_AUTH_EMULATOR_HOST(=firebase.auth.emulator-host)가 필수입니다."
                );
            }
            if (!StringUtils.hasText(properties.getProjectId())) {
                throw new IllegalStateException(
                        "local-emulator 프로필에서는 FIREBASE_PROJECT_ID(=firebase.auth.project-id)가 필수입니다."
                );
            }
            log.info("Firebase Auth emulator mode enabled. host={}, projectId={}",
                    properties.getEmulatorHost(),
                    properties.getProjectId());
            return;
        }

        if (emulatorHostConfigured) {
            throw new IllegalStateException(
                    "firebase.auth.use-emulator=false 인데 emulator host가 설정되었습니다. local-emulator 프로필로 실행하거나 host 설정을 제거하세요."
            );
        }
    }
}
