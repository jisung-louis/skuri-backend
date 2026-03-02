package com.skuri.skuri_backend.infra.auth.firebase;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;

public class InvalidFirebaseTokenException extends BusinessException {

    public InvalidFirebaseTokenException() {
        super(ErrorCode.UNAUTHORIZED, "유효하지 않은 인증 토큰입니다.");
    }

    public InvalidFirebaseTokenException(String message) {
        super(ErrorCode.UNAUTHORIZED, message);
    }
}
