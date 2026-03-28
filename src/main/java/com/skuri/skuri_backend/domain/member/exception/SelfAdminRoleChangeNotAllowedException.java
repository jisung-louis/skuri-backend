package com.skuri.skuri_backend.domain.member.exception;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;

public class SelfAdminRoleChangeNotAllowedException extends BusinessException {

    public SelfAdminRoleChangeNotAllowedException() {
        super(ErrorCode.SELF_ADMIN_ROLE_CHANGE_NOT_ALLOWED);
    }
}
