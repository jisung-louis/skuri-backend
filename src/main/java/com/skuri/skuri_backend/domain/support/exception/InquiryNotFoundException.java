package com.skuri.skuri_backend.domain.support.exception;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;

public class InquiryNotFoundException extends BusinessException {

    public InquiryNotFoundException() {
        super(ErrorCode.INQUIRY_NOT_FOUND);
    }
}
