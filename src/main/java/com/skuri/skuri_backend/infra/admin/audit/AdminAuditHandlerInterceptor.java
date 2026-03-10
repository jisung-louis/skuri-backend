package com.skuri.skuri_backend.infra.admin.audit;

import com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMember;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAuditHandlerInterceptor implements HandlerInterceptor {

    static final String REQUEST_CONTEXT_ATTRIBUTE = AdminAuditHandlerInterceptor.class.getName() + ".REQUEST_CONTEXT";

    private final BeanFactory beanFactory;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        AdminAudit adminAudit = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), AdminAudit.class);
        if (adminAudit == null) {
            return true;
        }

        AdminAuditRequestContext context = new AdminAuditRequestContext(adminAudit);
        context.setActorId(resolveActorId());
        request.setAttribute(REQUEST_CONTEXT_ATTRIBUTE, context);
        prepareBeforeSnapshot(request, context);
        return true;
    }

    void prepareBeforeSnapshot(HttpServletRequest request, AdminAuditRequestContext context) {
        if (context == null || context.isBeforePrepared()) {
            return;
        }

        if (!context.hasBeforeExpression()) {
            context.markBeforePrepared(null);
            return;
        }

        String targetId = resolveTargetId(request, context, null);
        if (StringUtils.hasText(targetId)) {
            context.updateTargetId(targetId);
        }

        if (!StringUtils.hasText(context.getTargetId()) && context.getRequestBody() == null) {
            return;
        }

        try {
            Object beforeSnapshot = evaluateExpression(request, context.getBeforeExpression(), context.getRequestBody(), null);
            context.markBeforePrepared(beforeSnapshot);
        } catch (RuntimeException e) {
            log.warn("관리자 감사 로그 before snapshot 계산에 실패했습니다. action={}, targetType={}", context.getAction(), context.getTargetType(), e);
            context.markBeforePrepared(null);
        }
    }

    String resolveTargetId(HttpServletRequest request, AdminAuditRequestContext context, Object responseBody) {
        if (StringUtils.hasText(context.getTargetId())) {
            return context.getTargetId();
        }
        Object evaluated;
        try {
            evaluated = AdminAuditExpressionEvaluator.evaluate(
                    beanFactory,
                    request,
                    context.getTargetIdExpression(),
                    context.getRequestBody(),
                    responseBody
            );
        } catch (org.springframework.expression.spel.SpelEvaluationException e) {
            if (responseBody == null) {
                return null;
            }
            throw e;
        }
        if (evaluated == null) {
            return null;
        }
        return String.valueOf(evaluated);
    }

    private String resolveActorId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedMember authenticatedMember) {
            return authenticatedMember.uid();
        }
        return null;
    }

    private Object evaluateExpression(
            HttpServletRequest request,
            String expression,
            Object requestBody,
            Object responseBody
    ) {
        return AdminAuditExpressionEvaluator.evaluate(beanFactory, request, expression, requestBody, responseBody);
    }
}
