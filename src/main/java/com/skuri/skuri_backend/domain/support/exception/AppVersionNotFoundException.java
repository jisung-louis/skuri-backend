package com.skuri.skuri_backend.domain.support.exception;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;

public class AppVersionNotFoundException extends BusinessException {

    public AppVersionNotFoundException() {
        super(ErrorCode.APP_VERSION_NOT_FOUND);
    }
}
