package com.skuri.skuri_backend.domain.taxiparty.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "관리자 파티 시스템 메시지 전송 요청")
public record CreateAdminPartySystemMessageRequest(
        @NotBlank(message = "message는 필수입니다.")
        @Size(max = 500, message = "message는 500자 이하여야 합니다.")
        @Schema(description = "관리자 안내 메시지", example = "관리자 안내 메시지")
        String message
) {
}
