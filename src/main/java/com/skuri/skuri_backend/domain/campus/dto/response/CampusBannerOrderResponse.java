package com.skuri.skuri_backend.domain.campus.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "캠퍼스 홈 배너 순서 응답")
public record CampusBannerOrderResponse(
        @Schema(description = "배너 ID", example = "campus_banner_uuid_2")
        String id,
        @Schema(description = "변경된 노출 순서", example = "1")
        int displayOrder
) {
}
