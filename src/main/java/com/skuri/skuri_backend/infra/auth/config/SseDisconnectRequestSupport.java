package com.skuri.skuri_backend.infra.auth.config;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Locale;

public final class SseDisconnectRequestSupport {

    private static final String SSE_URI_PREFIX = "/v1/sse/";

    private SseDisconnectRequestSupport() {
    }

    public static boolean isDisconnectedSseErrorDispatch(HttpServletRequest request) {
        return request.getDispatcherType() == DispatcherType.ERROR
                && isSseRequest(resolveOriginalRequestUri(request))
                && hasDisconnectedClientCause(resolveErrorThrowable(request), resolveErrorMessage(request));
    }

    static String resolveOriginalRequestUri(HttpServletRequest request) {
        Object originalRequestUri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        if (originalRequestUri instanceof String value && !value.isBlank()) {
            return value;
        }
        return request.getRequestURI();
    }

    private static Throwable resolveErrorThrowable(HttpServletRequest request) {
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        if (exception instanceof Throwable throwable) {
            return throwable;
        }
        return null;
    }

    private static String resolveErrorMessage(HttpServletRequest request) {
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        if (message instanceof String value) {
            return value;
        }
        return null;
    }

    private static boolean isSseRequest(String requestUri) {
        return requestUri != null && requestUri.startsWith(SSE_URI_PREFIX);
    }

    private static boolean hasDisconnectedClientCause(Throwable throwable, String errorMessage) {
        if (containsDisconnectedClientMarker(errorMessage)) {
            return true;
        }

        Throwable current = throwable;
        while (current != null) {
            if (containsDisconnectedClientMarker(current.getClass().getName())
                    || containsDisconnectedClientMarker(current.getMessage())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static boolean containsDisconnectedClientMarker(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.contains("asyncrequestnotusableexception")
                || normalized.contains("response not usable after response errors")
                || normalized.contains("broken pipe")
                || normalized.contains("connection reset by peer")
                || normalized.contains("connection reset")
                || normalized.contains("disconnected client")
                || normalized.contains("clientabortexception");
    }
}
