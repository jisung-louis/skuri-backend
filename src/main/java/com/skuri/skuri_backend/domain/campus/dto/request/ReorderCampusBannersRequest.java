package com.skuri.skuri_backend.domain.campus.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "캠퍼스 홈 배너 순서 변경 요청")
public record ReorderCampusBannersRequest(
        @NotEmpty(message = "bannerIds는 비어 있을 수 없습니다.")
        @Size(max = 100, message = "bannerIds는 최대 100개까지 전달할 수 있습니다.")
        @Schema(description = "변경할 전체 배너 ID 목록", example = "[\"campus_banner_uuid_2\",\"campus_banner_uuid_1\",\"campus_banner_uuid_3\"]")
        List<@NotBlank(message = "bannerIds 항목은 비어 있을 수 없습니다.") String> bannerIds
) {
}
