package com.skuri.skuri_backend.infra.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skuri.skuri_backend.common.config.ObjectMapperConfig;
import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.infra.auth.firebase.EmailDomainRestrictedException;
import com.skuri.skuri_backend.infra.auth.firebase.WithdrawnMemberAccessDeniedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperConfig.SHARED_OBJECT_MAPPER;
    private static final String ADMIN_PATH_PREFIX = "/v1/admin/";

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        ErrorCode errorCode;
        if (accessDeniedException instanceof EmailDomainRestrictedException) {
            errorCode = ErrorCode.EMAIL_DOMAIN_RESTRICTED;
        } else if (accessDeniedException instanceof WithdrawnMemberAccessDeniedException) {
            errorCode = ErrorCode.MEMBER_WITHDRAWN;
        } else if (isAdminPath(request)) {
            errorCode = ErrorCode.ADMIN_REQUIRED;
        } else {
            errorCode = ErrorCode.FORBIDDEN;
        }

        writeResponse(
                response,
                errorCode.getHttpStatus().value(),
                ApiResponse.error(errorCode.getCode(), errorCode.getMessage())
        );
    }

    private void writeResponse(HttpServletResponse response, int status, ApiResponse<Void> body) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(body));
    }

    private boolean isAdminPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null && uri.startsWith(ADMIN_PATH_PREFIX);
    }
}
