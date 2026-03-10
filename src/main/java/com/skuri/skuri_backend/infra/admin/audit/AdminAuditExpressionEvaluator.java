package com.skuri.skuri_backend.infra.admin.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

public final class AdminAuditExpressionEvaluator {

    private static final ExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();

    private AdminAuditExpressionEvaluator() {
    }

    public static Object evaluate(
            BeanFactory beanFactory,
            HttpServletRequest request,
            String expression,
            Object requestBody,
            Object responseBody
    ) {
        if (!StringUtils.hasText(expression)) {
            return null;
        }

        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setBeanResolver(new BeanFactoryResolver(beanFactory));
        context.setVariable("request", request);
        context.setVariable("requestBody", requestBody);
        context.setVariable("responseBody", responseBody);

        Map<String, String> pathVariables = extractPathVariables(request);
        context.setVariable("pathVariables", pathVariables);
        pathVariables.forEach(context::setVariable);

        request.getParameterMap().forEach((name, values) -> {
            if (values == null || values.length == 0) {
                return;
            }
            context.setVariable(name, values.length == 1 ? values[0] : values);
        });

        return EXPRESSION_PARSER.parseExpression(expression).getValue(context);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> extractPathVariables(HttpServletRequest request) {
        Object attribute = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (attribute instanceof Map<?, ?> variables) {
            return (Map<String, String>) variables;
        }
        return Map.of();
    }
}
