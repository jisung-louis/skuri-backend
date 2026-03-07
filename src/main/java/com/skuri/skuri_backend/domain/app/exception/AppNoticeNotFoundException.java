package com.skuri.skuri_backend.domain.app.exception;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;

public class AppNoticeNotFoundException extends BusinessException {

    public AppNoticeNotFoundException() {
        super(ErrorCode.APP_NOTICE_NOT_FOUND);
    }
}
