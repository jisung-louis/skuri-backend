package com.skuri.skuri_backend.domain.taxiparty.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "파티 위치 응답")
public record PartyLocationResponse(
        @Schema(description = "장소 이름", example = "성결대학교")
        String name,
        @Schema(description = "위도", example = "37.382742")
        Double lat,
        @Schema(description = "경도", example = "126.928031")
        Double lng
) {
}
