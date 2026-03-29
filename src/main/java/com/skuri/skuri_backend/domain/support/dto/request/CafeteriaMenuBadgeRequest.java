package com.skuri.skuri_backend.domain.support.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "학식 보조 태그 요청")
public record CafeteriaMenuBadgeRequest(
        @Schema(description = "보조 태그 코드. 생략 시 label 기반으로 자동 생성됩니다.", example = "TAKEOUT", nullable = true)
        String code,

        @NotBlank(message = "badge.label은 필수입니다.")
        @Schema(description = "보조 태그 라벨", example = "테이크아웃")
        String label
) {
}
