package com.skuri.skuri_backend.domain.image.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Schema(description = "이미지 업로드 컨텍스트", allowableValues = {
        "POST_IMAGE",
        "CHAT_IMAGE",
        "APP_NOTICE_IMAGE",
        "CAMPUS_BANNER_IMAGE",
        "PROFILE_IMAGE",
        "INQUIRY_IMAGE"
})
public enum ImageUploadContext {

    POST_IMAGE("posts", false),
    CHAT_IMAGE("chat", false),
    APP_NOTICE_IMAGE("app-notices", true),
    CAMPUS_BANNER_IMAGE("campus-banners", true),
    PROFILE_IMAGE("profiles", false),
    INQUIRY_IMAGE("inquiries", false);

    private final String directoryName;
    private final boolean adminOnly;

    ImageUploadContext(String directoryName, boolean adminOnly) {
        this.directoryName = directoryName;
        this.adminOnly = adminOnly;
    }

    public String directoryName() {
        return directoryName;
    }

    public boolean adminOnly() {
        return adminOnly;
    }

    public static ImageUploadContext from(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("context는 필수입니다.");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("지원하지 않는 context입니다: " + value);
        }
    }
}
