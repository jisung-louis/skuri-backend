package com.skuri.skuri_backend.domain.image.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

@Schema(description = "이미지 업로드 요청")
public record ImageUploadRequest(
        @Schema(description = "업로드할 이미지 파일", type = "string", format = "binary")
        MultipartFile file,

        @Schema(
                description = "업로드 컨텍스트",
                example = "POST_IMAGE",
                allowableValues = {"POST_IMAGE", "CHAT_IMAGE", "APP_NOTICE_IMAGE", "CAMPUS_BANNER_IMAGE", "PROFILE_IMAGE"}
        )
        String context
) {
}
