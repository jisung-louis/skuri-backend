package com.skuri.skuri_backend.domain.academic.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.regex.Pattern;

public final class AcademicSemesterResolver {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final Pattern SEMESTER_PATTERN = Pattern.compile("^\\d{4}-(1|2)$");

    private AcademicSemesterResolver() {
    }

    public static String resolve(String semester, boolean defaultCurrent) {
        String normalized = trimToNull(semester);
        if (normalized == null) {
            if (defaultCurrent) {
                return currentSemester();
            }
            return null;
        }
        if (!SEMESTER_PATTERN.matcher(normalized).matches()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "semester는 yyyy-1 또는 yyyy-2 형식이어야 합니다.");
        }
        return normalized;
    }

    public static String require(String semester) {
        String normalized = resolve(semester, false);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "semester는 필수입니다.");
        }
        return normalized;
    }

    public static String currentSemester() {
        return from(LocalDate.now(KOREA_ZONE));
    }

    public static String from(LocalDate date) {
        int month = date.getMonthValue();
        if (month == 1) {
            return (date.getYear() - 1) + "-2";
        }
        if (month <= 7) {
            return date.getYear() + "-1";
        }
        return date.getYear() + "-2";
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
