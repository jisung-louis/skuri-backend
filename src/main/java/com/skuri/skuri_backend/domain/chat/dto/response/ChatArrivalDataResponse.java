package com.skuri.skuri_backend.domain.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "ARRIVED 메시지 정산 정보")
public record ChatArrivalDataResponse(
        @Schema(description = "택시비", example = "14000", nullable = true)
        Integer taxiFare,
        @Schema(description = "1인당 금액", example = "4600", nullable = true)
        Integer perPerson,
        @Schema(description = "파티 인원 수", example = "4", nullable = true)
        Integer memberCount
) {
}
