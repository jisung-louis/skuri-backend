package com.skuri.skuri_backend.domain.support.exception;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;

public class CafeteriaMenuNotFoundException extends BusinessException {

    public CafeteriaMenuNotFoundException() {
        super(ErrorCode.CAFETERIA_MENU_NOT_FOUND);
    }
}
