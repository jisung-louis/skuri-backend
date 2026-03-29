package com.skuri.skuri_backend.domain.member.constant;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import org.springframework.util.StringUtils;

import java.util.Arrays;

public enum AdminMemberSortField {

    ID("id"),
    REALNAME("realname"),
    EMAIL("email"),
    NICKNAME("nickname"),
    DEPARTMENT("department"),
    STUDENT_ID("studentId"),
    JOINED_AT("joinedAt"),
    LAST_LOGIN("lastLogin"),
    LAST_LOGIN_OS("lastLoginOs");

    private final String parameterValue;

    AdminMemberSortField(String parameterValue) {
        this.parameterValue = parameterValue;
    }

    public String parameterValue() {
        return parameterValue;
    }

    public static AdminMemberSortField from(String value) {
        if (!StringUtils.hasText(value)) {
            return JOINED_AT;
        }

        return Arrays.stream(values())
                .filter(field -> field.parameterValue.equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR, "지원하지 않는 sortBy입니다."));
    }
}
