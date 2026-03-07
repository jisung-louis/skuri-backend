package com.skuri.skuri_backend.domain.support.exception;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;

public class ReportNotFoundException extends BusinessException {

    public ReportNotFoundException() {
        super(ErrorCode.REPORT_NOT_FOUND);
    }
}
