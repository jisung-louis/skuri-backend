package com.skuri.skuri_backend.domain.member.exception;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;

public class MemberActivityNotAvailableForWithdrawnException extends BusinessException {

    public MemberActivityNotAvailableForWithdrawnException() {
        super(ErrorCode.MEMBER_ACTIVITY_NOT_AVAILABLE_FOR_WITHDRAWN);
    }
}
