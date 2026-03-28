package com.skuri.skuri_backend.domain.app.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "미읽음 앱 공지 수 응답")
public record AppNoticeUnreadCountResponse(
        @Schema(description = "미읽음 앱 공지 수", example = "2")
        long count
) {
}
