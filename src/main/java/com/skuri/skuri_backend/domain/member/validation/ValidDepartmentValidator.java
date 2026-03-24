package com.skuri.skuri_backend.domain.member.validation;

import com.skuri.skuri_backend.domain.member.constant.DepartmentCatalog;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

public class ValidDepartmentValidator implements ConstraintValidator<ValidDepartment, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (!StringUtils.hasText(value)) {
            return true;
        }
        return DepartmentCatalog.isSupported(value);
    }
}
