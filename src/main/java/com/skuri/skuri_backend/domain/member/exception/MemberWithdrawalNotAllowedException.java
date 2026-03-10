package com.skuri.skuri_backend.domain.member.exception;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;

public class MemberWithdrawalNotAllowedException extends BusinessException {

    public MemberWithdrawalNotAllowedException(String message) {
        super(ErrorCode.MEMBER_WITHDRAWAL_NOT_ALLOWED, message);
    }
}
