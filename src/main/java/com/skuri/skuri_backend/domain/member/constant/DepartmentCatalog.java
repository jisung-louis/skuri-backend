package com.skuri.skuri_backend.domain.member.constant;

import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DepartmentCatalog {

    public static final List<String> DEPARTMENTS = List.of(
            "신학과",
            "기독교교육상담학과",
            "문화선교학과",
            "영어영문학과",
            "중어중문학과",
            "국어국문학과",
            "사회복지학과",
            "국제개발협력학과",
            "행정학과",
            "관광학과",
            "경영학과",
            "글로벌물류학과",
            "산업경영공학과",
            "유아교육과",
            "체육교육과",
            "교직부",
            "컴퓨터공학과",
            "정보통신공학과",
            "미디어소프트웨어학과",
            "도시디자인정보공학과",
            "음악학부",
            "실용음악과",
            "공연음악예술학부",
            "연기예술학과",
            "영화영상학과",
            "연극영화학부",
            "뷰티디자인학과",
            "융합학부",
            "파이데이아학부"
    );

    private static final Map<String, String> LEGACY_ALIASES = Map.of(
            "소프트웨어학과", "미디어소프트웨어학과"
    );

    private static final Map<String, String> SUPPORTED_DEPARTMENTS = buildSupportedDepartments();

    private DepartmentCatalog() {
    }

    public static boolean isSupported(String department) {
        return normalize(department) != null;
    }

    public static String normalize(String department) {
        if (!StringUtils.hasText(department)) {
            return null;
        }
        return SUPPORTED_DEPARTMENTS.get(department.trim());
    }

    private static Map<String, String> buildSupportedDepartments() {
        Map<String, String> supportedDepartments = new LinkedHashMap<>();
        for (String department : DEPARTMENTS) {
            supportedDepartments.put(department, department);
        }
        supportedDepartments.putAll(LEGACY_ALIASES);
        return Map.copyOf(supportedDepartments);
    }
}
