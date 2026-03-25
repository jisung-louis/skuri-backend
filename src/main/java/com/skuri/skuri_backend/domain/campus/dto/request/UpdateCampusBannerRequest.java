package com.skuri.skuri_backend.domain.campus.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.skuri.skuri_backend.domain.campus.entity.CampusBannerActionTarget;
import com.skuri.skuri_backend.domain.campus.entity.CampusBannerActionType;
import com.skuri.skuri_backend.domain.campus.entity.CampusBannerPaletteKey;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Schema(description = "캠퍼스 홈 배너 부분 수정 요청. 전달한 필드만 반영하며, `null`도 명시적 값으로 처리됩니다.")
@JsonIgnoreProperties(ignoreUnknown = false)
public class UpdateCampusBannerRequest {

    @Size(max = 50, message = "badgeLabel은 50자 이하여야 합니다.")
    @Schema(description = "배지 라벨", nullable = true, example = "공지사항")
    private String badgeLabel;

    @JsonIgnore
    private boolean badgeLabelPresent;

    @Size(max = 100, message = "titleLabel은 100자 이하여야 합니다.")
    @Schema(description = "제목 라벨", nullable = true, example = "학교 공지사항")
    private String titleLabel;

    @JsonIgnore
    private boolean titleLabelPresent;

    @Size(max = 200, message = "descriptionLabel은 200자 이하여야 합니다.")
    @Schema(description = "설명 라벨", nullable = true, example = "중요한 학교 소식을 놓치지 말고 확인하세요")
    private String descriptionLabel;

    @JsonIgnore
    private boolean descriptionLabelPresent;

    @Size(max = 50, message = "buttonLabel은 50자 이하여야 합니다.")
    @Schema(description = "버튼 라벨", nullable = true, example = "공지 보기")
    private String buttonLabel;

    @JsonIgnore
    private boolean buttonLabelPresent;

    @Schema(description = "팔레트 키", nullable = true, example = "BLUE")
    private CampusBannerPaletteKey paletteKey;

    @JsonIgnore
    private boolean paletteKeyPresent;

    @Size(max = 500, message = "imageUrl은 500자 이하여야 합니다.")
    @Schema(description = "배너 이미지 URL", nullable = true, example = "https://cdn.skuri.app/uploads/campus-banners/2026/03/25/banner-2.jpg")
    private String imageUrl;

    @JsonIgnore
    private boolean imageUrlPresent;

    @Schema(description = "액션 타입", nullable = true, example = "IN_APP")
    private CampusBannerActionType actionType;

    @JsonIgnore
    private boolean actionTypePresent;

    @Schema(description = "인앱 이동 대상", nullable = true, example = "NOTICE_MAIN")
    private CampusBannerActionTarget actionTarget;

    @JsonIgnore
    private boolean actionTargetPresent;

    @Schema(description = "인앱 이동 추가 파라미터 JSON 객체", nullable = true, type = "object", example = "{\"initialView\":\"all\"}")
    private JsonNode actionParams;

    @JsonIgnore
    private boolean actionParamsPresent;

    @Size(max = 500, message = "actionUrl은 500자 이하여야 합니다.")
    @Schema(description = "외부 이동 URL", nullable = true, example = "https://www.sungkyul.ac.kr")
    private String actionUrl;

    @JsonIgnore
    private boolean actionUrlPresent;

    @Schema(description = "활성 여부", nullable = true, example = "true")
    private Boolean isActive;

    @JsonIgnore
    private boolean activePresent;

    @Schema(description = "노출 시작 시각", nullable = true, example = "2026-03-25T00:00:00")
    private LocalDateTime displayStartAt;

    @JsonIgnore
    private boolean displayStartAtPresent;

    @Schema(description = "노출 종료 시각", nullable = true, example = "2026-04-30T23:59:59")
    private LocalDateTime displayEndAt;

    @JsonIgnore
    private boolean displayEndAtPresent;

    @JsonSetter("badgeLabel")
    public void setBadgeLabel(String badgeLabel) {
        this.badgeLabel = badgeLabel;
        this.badgeLabelPresent = true;
    }

    @JsonSetter("titleLabel")
    public void setTitleLabel(String titleLabel) {
        this.titleLabel = titleLabel;
        this.titleLabelPresent = true;
    }

    @JsonSetter("descriptionLabel")
    public void setDescriptionLabel(String descriptionLabel) {
        this.descriptionLabel = descriptionLabel;
        this.descriptionLabelPresent = true;
    }

    @JsonSetter("buttonLabel")
    public void setButtonLabel(String buttonLabel) {
        this.buttonLabel = buttonLabel;
        this.buttonLabelPresent = true;
    }

    @JsonSetter("paletteKey")
    public void setPaletteKey(CampusBannerPaletteKey paletteKey) {
        this.paletteKey = paletteKey;
        this.paletteKeyPresent = true;
    }

    @JsonSetter("imageUrl")
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
        this.imageUrlPresent = true;
    }

    @JsonSetter("actionType")
    public void setActionType(CampusBannerActionType actionType) {
        this.actionType = actionType;
        this.actionTypePresent = true;
    }

    @JsonSetter("actionTarget")
    public void setActionTarget(CampusBannerActionTarget actionTarget) {
        this.actionTarget = actionTarget;
        this.actionTargetPresent = true;
    }

    @JsonSetter("actionParams")
    public void setActionParams(JsonNode actionParams) {
        this.actionParams = actionParams;
        this.actionParamsPresent = true;
    }

    @JsonSetter("actionUrl")
    public void setActionUrl(String actionUrl) {
        this.actionUrl = actionUrl;
        this.actionUrlPresent = true;
    }

    @JsonSetter("isActive")
    public void setIsActive(Boolean active) {
        this.isActive = active;
        this.activePresent = true;
    }

    @JsonSetter("displayStartAt")
    public void setDisplayStartAt(LocalDateTime displayStartAt) {
        this.displayStartAt = displayStartAt;
        this.displayStartAtPresent = true;
    }

    @JsonSetter("displayEndAt")
    public void setDisplayEndAt(LocalDateTime displayEndAt) {
        this.displayEndAt = displayEndAt;
        this.displayEndAtPresent = true;
    }

    @JsonIgnore
    @AssertTrue(message = "최소 하나 이상의 수정 필드를 전달해야 합니다.")
    public boolean isUpdatableFieldPresent() {
        return badgeLabelPresent
                || titleLabelPresent
                || descriptionLabelPresent
                || buttonLabelPresent
                || paletteKeyPresent
                || imageUrlPresent
                || actionTypePresent
                || actionTargetPresent
                || actionParamsPresent
                || actionUrlPresent
                || activePresent
                || displayStartAtPresent
                || displayEndAtPresent;
    }
}
