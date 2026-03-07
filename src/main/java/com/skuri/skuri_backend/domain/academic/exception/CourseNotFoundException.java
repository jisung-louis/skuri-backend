package com.skuri.skuri_backend.domain.academic.exception;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;

public class CourseNotFoundException extends BusinessException {

    public CourseNotFoundException() {
        super(ErrorCode.COURSE_NOT_FOUND);
    }
}
