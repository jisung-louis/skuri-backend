package com.skuri.skuri_backend.infra.auth.config;

import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.util.Arrays;

final class FirebaseAuthRuntimeValidator {

    private static final String LOCAL_EMULATOR_PROFILE = "local-emulator";
    private static final String FIREBASE_AUTH_USE_EMULATOR = "FIREBASE_AUTH_USE_EMULATOR";
    private static final String FIREBASE_CREDENTIALS_PATH = "FIREBASE_CREDENTIALS_PATH";
    private static final String GOOGLE_APPLICATION_CREDENTIALS = "GOOGLE_APPLICATION_CREDENTIALS";

    private FirebaseAuthRuntimeValidator() {
    }

    static void validate(Environment environment, FirebaseAuthProperties properties) {
        boolean localEmulatorProfileActive = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(LOCAL_EMULATOR_PROFILE::equals);
        boolean useEmulator = properties.isUseEmulator();
        boolean emulatorHostConfigured = StringUtils.hasText(properties.getEmulatorHost());
        boolean credentialsPathConfigured = StringUtils.hasText(properties.getCredentialsPath());
        boolean googleApplicationCredentialsConfigured = StringUtils.hasText(
                environment.getProperty(GOOGLE_APPLICATION_CREDENTIALS)
        );

        if (localEmulatorProfileActive && !useEmulator) {
            StringBuilder message = new StringBuilder(
                    "local-emulator 프로필에서는 firebase.auth.use-emulator=true 이어야 합니다. "
                            + "IDE 환경변수나 쉘 export에서 "
                            + FIREBASE_AUTH_USE_EMULATOR
                            + "=false 로 덮어쓰지 마세요."
            );
            if (credentialsPathConfigured || googleApplicationCredentialsConfigured) {
                message.append(" 서비스 계정 경로 환경변수(")
                        .append(FIREBASE_CREDENTIALS_PATH)
                        .append(", ")
                        .append(GOOGLE_APPLICATION_CREDENTIALS)
                        .append(")가 감지되었습니다. ")
                        .append("IntelliJ 환경 변수 칸에는 .env 파일 경로가 아니라 KEY=value 형식으로 직접 입력해야 합니다.");
            }
            throw new IllegalStateException(message.toString());
        }

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
            return;
        }

        if (emulatorHostConfigured) {
            throw new IllegalStateException(
                    "firebase.auth.use-emulator=false 인데 emulator host가 설정되었습니다. local-emulator 프로필로 실행하거나 host 설정을 제거하세요."
            );
        }
    }
}
