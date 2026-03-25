package com.skuri.skuri_backend.domain.notice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "내 공지 북마크 목록 아이템 응답")
public record NoticeBookmarkSummaryResponse(
        @Schema(description = "공지 ID", example = "notice_id")
        String id,
        @Schema(description = "제목", example = "2026학년도 1학기 수강신청 안내")
        String title,
        @Schema(
                description = "RSS 미리보기 텍스트. 기존 Notice 공개 API와 동일한 필드명을 사용합니다.",
                example = "수강신청 일정, 대상 학년, 유의사항을 안내합니다."
        )
        String rssPreview,
        @Schema(description = "카테고리", example = "학사")
        String category,
        @Schema(
                description = "공지 게시 시각. 기존 Notice 공개 API와 동일한 시간 필드명을 사용합니다.",
                example = "2026-02-01T00:00:00"
        )
        LocalDateTime postedAt
) {
}
