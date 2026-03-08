package com.skuri.skuri_backend.domain.notification.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "FCM 토큰 등록 요청")
public record RegisterFcmTokenRequest(
        @NotBlank(message = "token은 필수입니다.")
        @Size(max = 500, message = "token은 500자를 초과할 수 없습니다.")
        @Schema(description = "디바이스 FCM 토큰", example = "dXZlbnQ6ZmNtLWRldmljZS10b2tlbg==")
        String token,
        @NotBlank(message = "platform은 필수입니다.")
        @Pattern(regexp = "ios|android", message = "platform은 ios 또는 android만 허용됩니다.")
        @Schema(description = "플랫폼", example = "ios")
        String platform
) {
}
