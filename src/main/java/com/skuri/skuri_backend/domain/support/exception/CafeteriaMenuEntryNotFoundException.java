package com.skuri.skuri_backend.domain.support.exception;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;

public class CafeteriaMenuEntryNotFoundException extends BusinessException {

    public CafeteriaMenuEntryNotFoundException() {
        super(ErrorCode.CAFETERIA_MENU_ENTRY_NOT_FOUND);
    }
}
