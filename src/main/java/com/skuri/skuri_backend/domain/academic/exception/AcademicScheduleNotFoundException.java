package com.skuri.skuri_backend.domain.academic.exception;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;

public class AcademicScheduleNotFoundException extends BusinessException {

    public AcademicScheduleNotFoundException() {
        super(ErrorCode.ACADEMIC_SCHEDULE_NOT_FOUND);
    }
}
