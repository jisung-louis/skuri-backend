package com.skuri.skuri_backend.domain.taxiparty.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "파티 위치 입력")
public record PartyLocationRequest(
        @Schema(description = "장소 이름", example = "성결대학교")
        @NotBlank(message = "name은 필수입니다.")
        @Size(max = 100, message = "name은 100자 이하여야 합니다.")
        String name,

        @Schema(description = "위도", example = "37.382742")
        @NotNull(message = "lat은 필수입니다.")
        Double lat,

        @Schema(description = "경도", example = "126.928031")
        @NotNull(message = "lng은 필수입니다.")
        Double lng
) {
}
