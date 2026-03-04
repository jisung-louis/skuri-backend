package com.skuri.skuri_backend.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "공통 API 응답 포맷")
public class ApiResponse<T> {

    @Schema(description = "요청 성공 여부")
    private final boolean success;
    @Schema(description = "성공 시 응답 데이터", nullable = true)
    private final T data;
    @Schema(description = "에러 메시지", nullable = true)
    private final String message;
    @Schema(description = "에러 코드", nullable = true)
    private final String errorCode;
    @Schema(description = "에러 발생 시각", nullable = true)
    private final LocalDateTime timestamp;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    public static ApiResponse<Void> error(String errorCode, String message) {
        return ApiResponse.<Void>builder()
                .success(false)
                .errorCode(errorCode)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
