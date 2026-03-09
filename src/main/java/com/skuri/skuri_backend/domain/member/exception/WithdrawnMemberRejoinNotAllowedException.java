package com.skuri.skuri_backend.domain.member.exception;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;

public class WithdrawnMemberRejoinNotAllowedException extends BusinessException {

    public WithdrawnMemberRejoinNotAllowedException() {
        super(ErrorCode.WITHDRAWN_MEMBER_REJOIN_NOT_ALLOWED);
    }
}
