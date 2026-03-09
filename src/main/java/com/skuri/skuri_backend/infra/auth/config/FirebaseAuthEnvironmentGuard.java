package com.skuri.skuri_backend.infra.auth.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FirebaseAuthEnvironmentGuard {

    private final Environment environment;
    private final FirebaseAuthProperties properties;

    @PostConstruct
    void validate() {
        FirebaseAuthRuntimeValidator.validate(environment, properties);
        if (properties.isUseEmulator()) {
            log.info("Firebase Auth emulator mode enabled. host={}, projectId={}",
                    properties.getEmulatorHost(),
                    properties.getProjectId());
        }
    }
}
