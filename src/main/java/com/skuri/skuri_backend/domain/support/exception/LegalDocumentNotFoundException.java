package com.skuri.skuri_backend.domain.support.exception;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;

public class LegalDocumentNotFoundException extends BusinessException {

    public LegalDocumentNotFoundException() {
        super(ErrorCode.LEGAL_DOCUMENT_NOT_FOUND);
    }
}
