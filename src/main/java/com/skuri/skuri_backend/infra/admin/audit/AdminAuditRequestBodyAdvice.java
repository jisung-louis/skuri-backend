package com.skuri.skuri_backend.infra.admin.audit;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

@ControllerAdvice
@lombok.RequiredArgsConstructor
public class AdminAuditRequestBodyAdvice extends RequestBodyAdviceAdapter {

    private final AdminAuditHandlerInterceptor adminAuditHandlerInterceptor;

    @Override
    public boolean supports(MethodParameter methodParameter, java.lang.reflect.Type targetType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return methodParameter.hasParameterAnnotation(RequestBody.class);
    }

    @Override
    public Object afterBodyRead(
            Object body,
            HttpInputMessage inputMessage,
            MethodParameter parameter,
            java.lang.reflect.Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType
    ) {
        if (!(inputMessage instanceof ServletServerHttpRequest servletRequest)) {
            return body;
        }

        Object attribute = servletRequest.getServletRequest().getAttribute(AdminAuditHandlerInterceptor.REQUEST_CONTEXT_ATTRIBUTE);
        if (attribute instanceof AdminAuditRequestContext context) {
            if (context.getRequestBody() == null) {
                context.setRequestBody(body);
            }
            adminAuditHandlerInterceptor.prepareBeforeSnapshot(servletRequest.getServletRequest(), context);
        }
        return body;
    }
}
