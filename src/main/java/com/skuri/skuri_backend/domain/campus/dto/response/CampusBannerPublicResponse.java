package com.skuri.skuri_backend.domain.campus.dto.response;

import com.skuri.skuri_backend.domain.campus.entity.CampusBannerActionTarget;
import com.skuri.skuri_backend.domain.campus.entity.CampusBannerActionType;
import com.skuri.skuri_backend.domain.campus.entity.CampusBannerPaletteKey;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "캠퍼스 홈 배너 공개 응답")
public record CampusBannerPublicResponse(
        @Schema(description = "배너 ID", example = "campus_banner_uuid_1")
        String id,
        @Schema(description = "배지 라벨", example = "택시 파티")
        String badgeLabel,
        @Schema(description = "제목 라벨", example = "택시 동승 매칭")
        String titleLabel,
        @Schema(description = "설명 라벨", example = "같은 방향 가는 학생과 택시비를 함께 나눠요")
        String descriptionLabel,
        @Schema(description = "버튼 라벨", example = "파티 찾기")
        String buttonLabel,
        @Schema(description = "팔레트 키", example = "GREEN")
        CampusBannerPaletteKey paletteKey,
        @Schema(description = "배너 이미지 URL", example = "https://cdn.skuri.app/uploads/campus-banners/2026/03/25/banner-1.jpg")
        String imageUrl,
        @Schema(description = "액션 타입", example = "IN_APP")
        CampusBannerActionType actionType,
        @Schema(description = "인앱 이동 대상", nullable = true, example = "TAXI_MAIN")
        CampusBannerActionTarget actionTarget,
        @Schema(description = "인앱 이동 추가 파라미터 JSON 객체", nullable = true, type = "object", example = "{\"initialView\":\"all\"}")
        Map<String, Object> actionParams,
        @Schema(description = "외부 이동 URL", nullable = true, example = "https://www.sungkyul.ac.kr")
        String actionUrl
) {
}
