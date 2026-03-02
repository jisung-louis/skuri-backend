package com.skuri.skuri_backend.infra.auth.firebase;

import com.skuri.skuri_backend.common.exception.ErrorCode;
import org.springframework.security.access.AccessDeniedException;

public class EmailDomainRestrictedException extends AccessDeniedException {

    public EmailDomainRestrictedException() {
        super(ErrorCode.EMAIL_DOMAIN_RESTRICTED.getMessage());
    }
}
