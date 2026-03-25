package com.skuri.skuri_backend.domain.campus.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.skuri.skuri_backend.domain.campus.entity.CampusBannerActionTarget;
import com.skuri.skuri_backend.domain.campus.entity.CampusBannerActionType;
import com.skuri.skuri_backend.domain.campus.entity.CampusBannerPaletteKey;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Schema(description = "캠퍼스 홈 배너 생성 요청")
public record CreateCampusBannerRequest(
        @NotBlank(message = "badgeLabel은 필수입니다.")
        @Size(max = 50, message = "badgeLabel은 50자 이하여야 합니다.")
        @Schema(description = "배지 라벨", example = "택시 파티")
        String badgeLabel,

        @NotBlank(message = "titleLabel은 필수입니다.")
        @Size(max = 100, message = "titleLabel은 100자 이하여야 합니다.")
        @Schema(description = "제목 라벨", example = "택시 동승 매칭")
        String titleLabel,

        @NotBlank(message = "descriptionLabel은 필수입니다.")
        @Size(max = 200, message = "descriptionLabel은 200자 이하여야 합니다.")
        @Schema(description = "설명 라벨", example = "같은 방향 가는 학생과 택시비를 함께 나눠요")
        String descriptionLabel,

        @NotBlank(message = "buttonLabel은 필수입니다.")
        @Size(max = 50, message = "buttonLabel은 50자 이하여야 합니다.")
        @Schema(description = "버튼 라벨", example = "파티 찾기")
        String buttonLabel,

        @NotNull(message = "paletteKey는 필수입니다.")
        @Schema(description = "팔레트 키", example = "GREEN")
        CampusBannerPaletteKey paletteKey,

        @NotBlank(message = "imageUrl은 필수입니다.")
        @Size(max = 500, message = "imageUrl은 500자 이하여야 합니다.")
        @Schema(description = "배너 이미지 URL", example = "https://cdn.skuri.app/uploads/campus-banners/2026/03/25/banner-1.jpg")
        String imageUrl,

        @NotNull(message = "actionType은 필수입니다.")
        @Schema(description = "액션 타입", example = "IN_APP")
        CampusBannerActionType actionType,

        @Schema(description = "인앱 이동 대상", nullable = true, example = "TAXI_MAIN")
        CampusBannerActionTarget actionTarget,

        @Schema(description = "인앱 이동 추가 파라미터 JSON 객체", nullable = true, type = "object", example = "{\"initialView\":\"all\"}")
        JsonNode actionParams,

        @Size(max = 500, message = "actionUrl은 500자 이하여야 합니다.")
        @Schema(description = "외부 이동 URL", nullable = true, example = "https://www.sungkyul.ac.kr")
        String actionUrl,

        @NotNull(message = "isActive는 필수입니다.")
        @Schema(description = "활성 여부", example = "true")
        Boolean isActive,

        @Schema(description = "노출 시작 시각", nullable = true, example = "2026-03-25T00:00:00")
        LocalDateTime displayStartAt,

        @Schema(description = "노출 종료 시각", nullable = true, example = "2026-04-30T23:59:59")
        LocalDateTime displayEndAt
) {
}
