package com.skuri.skuri_backend.infra.admin.audit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skuri.skuri_backend.common.config.ObjectMapperConfig;
import com.skuri.skuri_backend.infra.auth.config.AdminRequestPaths;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAuditFilter extends OncePerRequestFilter {

    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperConfig.SHARED_OBJECT_MAPPER;
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final Set<String> AUDITABLE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final AdminAuditHandlerInterceptor adminAuditHandlerInterceptor;
    private final BeanFactory beanFactory;
    private final ObjectProvider<AdminAuditLogService> adminAuditLogServiceProvider;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !AdminRequestPaths.isAdminRequest(request)
                || !AUDITABLE_METHODS.contains(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(request, responseWrapper);
            recordAuditIfNecessary(request, responseWrapper);
        } finally {
            responseWrapper.copyBodyToResponse();
        }
    }

    private void recordAuditIfNecessary(HttpServletRequest request, ContentCachingResponseWrapper responseWrapper) {
        AdminAuditLogService adminAuditLogService = adminAuditLogServiceProvider.getIfAvailable();
        if (adminAuditLogService == null) {
            return;
        }
        Object attribute = request.getAttribute(AdminAuditHandlerInterceptor.REQUEST_CONTEXT_ATTRIBUTE);
        if (!(attribute instanceof AdminAuditRequestContext context)) {
            return;
        }
        if (responseWrapper.getStatus() >= 400) {
            return;
        }

        byte[] responseBodyBytes = responseWrapper.getContentAsByteArray();
        if (responseBodyBytes.length == 0) {
            return;
        }

        try {
            Map<String, Object> responseBody = OBJECT_MAPPER.readValue(
                    new String(responseBodyBytes, StandardCharsets.UTF_8),
                    MAP_TYPE
            );
            if (!Boolean.TRUE.equals(responseBody.get("success"))) {
                return;
            }

            adminAuditHandlerInterceptor.prepareBeforeSnapshot(request, context);
            String targetId = adminAuditHandlerInterceptor.resolveTargetId(request, context, responseBody);
            context.updateTargetId(targetId);

            Object afterSnapshot = null;
            if (context.hasAfterExpression()) {
                afterSnapshot = AdminAuditExpressionEvaluator.evaluate(
                        beanFactory,
                        request,
                        context.getAfterExpression(),
                        context.getRequestBody(),
                        responseBody
                );
            }

            adminAuditLogService.record(context, afterSnapshot);
        } catch (Exception e) {
            log.warn("관리자 감사 로그 저장에 실패했습니다. action={}, targetType={}", context.getAction(), context.getTargetType(), e);
        }
    }
}
