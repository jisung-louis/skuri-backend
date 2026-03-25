package com.skuri.skuri_backend.domain.notice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "공지 목록 아이템 응답")
public record NoticeSummaryResponse(
        @Schema(description = "공지 ID", example = "notice_id")
        String id,
        @Schema(description = "제목", example = "2026학년도 1학기 수강신청 안내")
        String title,
        @Schema(description = "RSS 미리보기 텍스트", example = "수강신청 일정, 대상 학년, 유의사항을 안내합니다.")
        String rssPreview,
        @Schema(description = "카테고리", example = "학사")
        String category,
        @Schema(description = "부서", example = "성결대학교")
        String department,
        @Schema(description = "작성자", example = "교무처")
        String author,
        @Schema(description = "게시 시각", example = "2026-02-01T00:00:00")
        LocalDateTime postedAt,
        @Schema(description = "조회수", example = "500")
        int viewCount,
        @Schema(description = "좋아요 수", example = "10")
        int likeCount,
        @Schema(description = "댓글 수", example = "10")
        int commentCount,
        @Schema(description = "북마크 수", example = "3")
        int bookmarkCount,
        @Schema(description = "읽음 여부", example = "true")
        boolean isRead,
        @Schema(description = "내 좋아요 여부", example = "false")
        boolean isLiked,
        @Schema(description = "내 북마크 여부", example = "true")
        boolean isBookmarked,
        @Schema(description = "내 댓글 작성 여부", example = "true")
        boolean isCommentedByMe
) {
}
