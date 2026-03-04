package com.skuri.skuri_backend.domain.taxiparty.exception;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;

public class JoinRequestNotFoundException extends BusinessException {

    public JoinRequestNotFoundException() {
        super(ErrorCode.REQUEST_NOT_FOUND);
    }
}
