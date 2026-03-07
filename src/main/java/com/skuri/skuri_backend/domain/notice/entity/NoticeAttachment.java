package com.skuri.skuri_backend.domain.notice.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공지 첨부파일")
public record NoticeAttachment(
        @Schema(description = "첨부파일명", example = "수강신청 안내.pdf")
        String name,
        @Schema(description = "다운로드 URL", example = "https://www.sungkyul.ac.kr/.../download.do")
        String downloadUrl,
        @Schema(description = "미리보기 URL", nullable = true, example = "https://www.sungkyul.ac.kr/.../synapView.do")
        String previewUrl
) {
}
