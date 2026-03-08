package com.skuri.skuri_backend.domain.notification.exception;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;

public class NotNotificationOwnerException extends BusinessException {

    public NotNotificationOwnerException() {
        super(ErrorCode.NOT_NOTIFICATION_OWNER);
    }
}
