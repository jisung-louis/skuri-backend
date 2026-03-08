package com.skuri.skuri_backend.infra.auth.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.AccessToken;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

@Slf4j
@Configuration
@EnableConfigurationProperties(FirebaseAuthProperties.class)
public class FirebaseConfig {

    @Bean
    @Conditional(FirebaseCredentialsCondition.class)
    public FirebaseApp firebaseApp(FirebaseAuthProperties properties) throws IOException {
        List<FirebaseApp> apps = FirebaseApp.getApps();
        if (!apps.isEmpty()) {
            return apps.getFirst();
        }

        GoogleCredentials credentials = loadCredentials(
                properties.getCredentialsPath(),
                properties.isUseEmulator()
        );

        FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder()
                .setCredentials(credentials);
        if (StringUtils.hasText(properties.getProjectId())) {
            optionsBuilder.setProjectId(properties.getProjectId());
        }

        FirebaseApp firebaseApp = FirebaseApp.initializeApp(optionsBuilder.build());
        log.info("FirebaseApp initialized");
        return firebaseApp;
    }

    @Bean
    @ConditionalOnBean(FirebaseApp.class)
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }

    @Bean
    @ConditionalOnBean(FirebaseApp.class)
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }

    private GoogleCredentials loadCredentials(String credentialsPath, boolean useEmulator) throws IOException {
        if (StringUtils.hasText(credentialsPath)) {
            try (InputStream inputStream = new FileInputStream(credentialsPath)) {
                return GoogleCredentials.fromStream(inputStream);
            }
        }
        if (useEmulator) {
            // Emulator mode does not require a real service account credential.
            AccessToken accessToken = new AccessToken(
                    "firebase-emulator",
                    Date.from(Instant.now().plus(3650, ChronoUnit.DAYS))
            );
            return GoogleCredentials.create(accessToken);
        }
        return GoogleCredentials.getApplicationDefault();
    }
}
