package com.skuri.skuri_backend.domain.academic.exception;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;

public class CourseAlreadyInTimetableException extends BusinessException {

    public CourseAlreadyInTimetableException() {
        super(ErrorCode.COURSE_ALREADY_IN_TIMETABLE);
    }
}
