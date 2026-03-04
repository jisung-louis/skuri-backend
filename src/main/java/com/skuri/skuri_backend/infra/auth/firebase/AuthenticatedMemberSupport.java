package com.skuri.skuri_backend.infra.auth.firebase;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;

public final class AuthenticatedMemberSupport {

    private AuthenticatedMemberSupport() {
    }

    public static AuthenticatedMember requireAuthenticatedMember(AuthenticatedMember authenticatedMember) {
        if (authenticatedMember == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return authenticatedMember;
    }
}
