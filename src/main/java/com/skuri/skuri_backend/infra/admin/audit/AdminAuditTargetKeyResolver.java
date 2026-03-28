package com.skuri.skuri_backend.infra.admin.audit;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.domain.academic.service.AcademicSemesterResolver;
import com.skuri.skuri_backend.domain.support.entity.AppPlatform;
import com.skuri.skuri_backend.domain.support.entity.LegalDocumentKey;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component("adminAuditTargetKeys")
public class AdminAuditTargetKeyResolver {

    public String courseSemester(String semester) {
        String trimmed = trimToNull(semester);
        if (trimmed == null) {
            return null;
        }
        try {
            return AcademicSemesterResolver.require(trimmed);
        } catch (BusinessException e) {
            return trimmed;
        }
    }

    public String appPlatform(String platform) {
        String trimmed = trimToNull(platform);
        if (trimmed == null) {
            return null;
        }
        try {
            return AppPlatform.from(trimmed).value();
        } catch (IllegalArgumentException e) {
            return trimmed;
        }
    }

    public String legalDocumentKey(String documentKey) {
        String trimmed = trimToNull(documentKey);
        if (trimmed == null) {
            return null;
        }
        try {
            return LegalDocumentKey.from(trimmed).value();
        } catch (IllegalArgumentException e) {
            return trimmed;
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
