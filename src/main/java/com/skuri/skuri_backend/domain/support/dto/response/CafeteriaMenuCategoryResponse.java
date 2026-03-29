package com.skuri.skuri_backend.domain.support.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "학식 카테고리 정의")
public record CafeteriaMenuCategoryResponse(
        @Schema(description = "카테고리 코드", example = "rollNoodles")
        String code,

        @Schema(description = "카테고리명", example = "Roll & Noodles")
        String label
) {
}
