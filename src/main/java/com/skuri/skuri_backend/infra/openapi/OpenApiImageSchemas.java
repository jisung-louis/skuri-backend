package com.skuri.skuri_backend.infra.openapi;

import com.skuri.skuri_backend.domain.image.dto.response.ImageUploadResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public final class OpenApiImageSchemas {

    private OpenApiImageSchemas() {
    }

    @Schema(name = "ImageUploadApiResponse", description = "공통 API 응답 포맷")
    public record ImageUploadApiResponse(
            @Schema(description = "요청 성공 여부")
            boolean success,
            @Schema(description = "성공 시 응답 데이터", nullable = true)
            ImageUploadResponse data,
            @Schema(description = "에러 메시지", nullable = true)
            String message,
            @Schema(description = "에러 코드", nullable = true)
            String errorCode,
            @Schema(description = "에러 발생 시각", nullable = true)
            LocalDateTime timestamp
    ) {
    }
}
