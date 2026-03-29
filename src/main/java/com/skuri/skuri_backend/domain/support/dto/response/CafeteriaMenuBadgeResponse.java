package com.skuri.skuri_backend.domain.support.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "학식 보조 태그 응답")
public record CafeteriaMenuBadgeResponse(
        @Schema(description = "보조 태그 코드", example = "TAKEOUT")
        String code,

        @Schema(description = "보조 태그 라벨", example = "테이크아웃")
        String label
) {
}
