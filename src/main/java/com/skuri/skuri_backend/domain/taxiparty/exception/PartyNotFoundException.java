package com.skuri.skuri_backend.domain.taxiparty.exception;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;

public class PartyNotFoundException extends BusinessException {

    public PartyNotFoundException() {
        super(ErrorCode.PARTY_NOT_FOUND);
    }
}
