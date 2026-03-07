package com.skuri.skuri_backend.domain.academic.exception;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;

public class TimetableConflictException extends BusinessException {

    public TimetableConflictException() {
        super(ErrorCode.TIMETABLE_CONFLICT);
    }
}
