package com.skuri.skuri_backend.domain.notification.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "FCM 토큰 삭제 요청")
public record DeleteFcmTokenRequest(
        @NotBlank(message = "token은 필수입니다.")
        @Size(max = 500, message = "token은 500자를 초과할 수 없습니다.")
        @Schema(description = "삭제할 FCM 토큰", example = "dXZlbnQ6ZmNtLWRldmljZS10b2tlbg==")
        String token
) {
}
