package com.skuri.skuri_backend.infra.auth.config;

import jakarta.servlet.http.HttpServletRequest;

public final class AdminRequestPaths {

    public static final String API_PREFIX = "/v1/admin/";

    private AdminRequestPaths() {
    }

    public static boolean isAdminRequest(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String uri = request.getRequestURI();
        return uri != null && uri.startsWith(API_PREFIX);
    }
}
