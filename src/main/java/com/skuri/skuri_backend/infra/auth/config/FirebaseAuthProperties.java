package com.skuri.skuri_backend.infra.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "firebase.auth")
public class FirebaseAuthProperties {

    private String credentialsPath;
    private String projectId;
    private boolean useEmulator;
    private String emulatorHost;
    private boolean autoStopOnShutdown;
    private String emulatorPidFile;
}
