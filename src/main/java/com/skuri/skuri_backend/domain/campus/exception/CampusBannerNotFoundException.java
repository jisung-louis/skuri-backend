package com.skuri.skuri_backend.domain.campus.exception;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;

public class CampusBannerNotFoundException extends BusinessException {

    public CampusBannerNotFoundException() {
        super(ErrorCode.CAMPUS_BANNER_NOT_FOUND);
    }
}
