package com.skuri.skuri_backend.infra.auth.config;

import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.infra.auth.firebase.EmailDomainRestrictedException;
import com.skuri.skuri_backend.infra.auth.firebase.WithdrawnMemberAccessDeniedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.AccessDeniedException;

public final class ApiAccessDeniedErrorResolver {

    private ApiAccessDeniedErrorResolver() {
    }

    public static ErrorCode resolve(HttpServletRequest request, AccessDeniedException exception) {
        if (exception instanceof EmailDomainRestrictedException) {
            return ErrorCode.EMAIL_DOMAIN_RESTRICTED;
        }
        if (exception instanceof WithdrawnMemberAccessDeniedException) {
            return ErrorCode.MEMBER_WITHDRAWN;
        }
        if (AdminRequestPaths.isAdminRequest(request)) {
            return ErrorCode.ADMIN_REQUIRED;
        }
        return ErrorCode.FORBIDDEN;
    }
}
