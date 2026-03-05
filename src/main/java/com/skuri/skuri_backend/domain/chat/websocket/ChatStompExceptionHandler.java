package com.skuri.skuri_backend.domain.chat.websocket;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class ChatStompExceptionHandler {

    @MessageExceptionHandler(BusinessException.class)
    @SendToUser("/queue/errors")
    public ApiResponse<Void> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("StompBusinessException: {} - {}", errorCode.getCode(), e.getMessage());
        return ApiResponse.error(errorCode.getCode(), e.getMessage());
    }

    @MessageExceptionHandler(MethodArgumentNotValidException.class)
    @SendToUser("/queue/errors")
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ApiResponse.error(ErrorCode.VALIDATION_ERROR.getCode(), message);
    }

    @MessageExceptionHandler(MessagingException.class)
    @SendToUser("/queue/errors")
    public ApiResponse<Void> handleMessagingException(MessagingException e) {
        BusinessException businessException = findBusinessException(e);
        if (businessException != null) {
            return ApiResponse.error(
                    businessException.getErrorCode().getCode(),
                    businessException.getMessage()
            );
        }
        return ApiResponse.error(ErrorCode.INTERNAL_ERROR.getCode(), ErrorCode.INTERNAL_ERROR.getMessage());
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser("/queue/errors")
    public ApiResponse<Void> handleException(Exception e) {
        log.error("StompUnhandledException", e);
        return ApiResponse.error(ErrorCode.INTERNAL_ERROR.getCode(), ErrorCode.INTERNAL_ERROR.getMessage());
    }

    private BusinessException findBusinessException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof BusinessException businessException) {
                return businessException;
            }
            current = current.getCause();
        }
        return null;
    }
}
