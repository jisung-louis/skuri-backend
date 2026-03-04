package com.skuri.skuri_backend.domain.taxiparty.dto.response;

import com.skuri.skuri_backend.domain.taxiparty.entity.JoinRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "동승 요청 처리 응답")
public record JoinRequestResponse(
        @Schema(description = "요청 ID", example = "request_uuid")
        String id,
        @Schema(description = "요청 상태", example = "PENDING")
        JoinRequestStatus status
) {
}
