package com.skuri.skuri_backend.infra.auth.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

public class FirebaseCredentialsCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String credentialsPath = context.getEnvironment().getProperty("firebase.auth.credentials-path");
        String adcPath = context.getEnvironment().getProperty("GOOGLE_APPLICATION_CREDENTIALS");
        return StringUtils.hasText(credentialsPath) || StringUtils.hasText(adcPath);
    }
}
