package com.skuri.skuri_backend.domain.taxiparty.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

@Schema(description = "도착 처리 요청")
public record ArrivePartyRequest(
        @Schema(description = "총 택시비", example = "14000")
        @Min(value = 1, message = "taxiFare는 1원 이상이어야 합니다.")
        int taxiFare
) {
}
