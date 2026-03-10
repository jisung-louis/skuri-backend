package com.skuri.skuri_backend.infra.admin.audit;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AdminAuditWebMvcConfig implements WebMvcConfigurer {

    private final AdminAuditHandlerInterceptor adminAuditHandlerInterceptor;

    public AdminAuditWebMvcConfig(AdminAuditHandlerInterceptor adminAuditHandlerInterceptor) {
        this.adminAuditHandlerInterceptor = adminAuditHandlerInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminAuditHandlerInterceptor)
                .addPathPatterns("/v1/admin/**");
    }
}
