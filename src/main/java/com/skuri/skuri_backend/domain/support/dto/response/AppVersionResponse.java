package com.skuri.skuri_backend.domain.support.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "앱 버전 응답")
public record AppVersionResponse(
        @Schema(description = "플랫폼", example = "ios")
        String platform,

        @Schema(description = "최소 허용 버전", example = "1.5.0")
        String minimumVersion,

        @Schema(description = "강제 업데이트 여부", example = "false")
        boolean forceUpdate,

        @Schema(description = "업데이트 안내 메시지", nullable = true, example = "새로운 기능이 추가되었습니다.")
        String message,

        @Schema(description = "업데이트 안내 제목", nullable = true, example = "업데이트 안내")
        String title,

        @Schema(description = "업데이트 버튼 노출 여부", example = "true")
        boolean showButton,

        @Schema(description = "버튼 문구", nullable = true, example = "업데이트")
        String buttonText,

        @Schema(description = "버튼 이동 URL", nullable = true, example = "https://apps.apple.com/...")
        String buttonUrl
) {
}
