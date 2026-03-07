package com.skuri.skuri_backend.domain.support.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.util.StringUtils;

@Schema(description = "앱 버전 저장 요청")
public record UpsertAppVersionRequest(
        @NotBlank(message = "minimumVersion은 필수입니다.")
        @Size(max = 20, message = "minimumVersion은 20자 이하여야 합니다.")
        @Schema(description = "최소 허용 버전", example = "1.6.0")
        String minimumVersion,

        @NotNull(message = "forceUpdate는 필수입니다.")
        @Schema(description = "강제 업데이트 여부", example = "true")
        Boolean forceUpdate,

        @Size(max = 100, message = "title은 100자 이하여야 합니다.")
        @Schema(description = "업데이트 안내 제목", nullable = true, example = "필수 업데이트 안내")
        String title,

        @Size(max = 500, message = "message는 500자 이하여야 합니다.")
        @Schema(description = "업데이트 안내 메시지", nullable = true, example = "안정성 개선을 위한 필수 업데이트입니다.")
        String message,

        @NotNull(message = "showButton은 필수입니다.")
        @Schema(description = "업데이트 버튼 노출 여부", example = "true")
        Boolean showButton,

        @Size(max = 100, message = "buttonText는 100자 이하여야 합니다.")
        @Schema(description = "버튼 문구", nullable = true, example = "업데이트")
        String buttonText,

        @Size(max = 500, message = "buttonUrl은 500자 이하여야 합니다.")
        @Schema(description = "버튼 이동 URL", nullable = true, example = "https://apps.apple.com/...")
        String buttonUrl
) {

    @AssertTrue(message = "showButton이 true이면 buttonText와 buttonUrl이 모두 필요합니다.")
    @Schema(hidden = true)
    public boolean isButtonConfigurationValid() {
        if (!Boolean.TRUE.equals(showButton)) {
            return true;
        }
        return StringUtils.hasText(buttonText) && StringUtils.hasText(buttonUrl);
    }
}
