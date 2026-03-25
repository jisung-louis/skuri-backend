package com.skuri.skuri_backend.domain.notice.dto.response;

import com.skuri.skuri_backend.domain.notice.entity.NoticeAttachment;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "공지 상세 응답")
public record NoticeDetailResponse(
        @Schema(description = "공지 ID", example = "notice_id")
        String id,
        @Schema(description = "제목", example = "2026학년도 1학기 수강신청 안내")
        String title,
        @Schema(description = "RSS 미리보기 텍스트", example = "수강신청 안내 요약 내용")
        String rssPreview,
        @Schema(description = "상세 HTML 본문", example = "<p>상세 본문</p>")
        String bodyHtml,
        @Schema(description = "원문 링크", example = "https://www.sungkyul.ac.kr/...")
        String link,
        @Schema(description = "카테고리", example = "학사")
        String category,
        @Schema(description = "부서", example = "성결대학교")
        String department,
        @Schema(description = "작성자", example = "교무처")
        String author,
        @Schema(description = "출처", example = "RSS")
        String source,
        @Schema(description = "게시 시각", example = "2026-02-01T00:00:00")
        LocalDateTime postedAt,
        @Schema(description = "조회수", example = "501")
        int viewCount,
        @Schema(description = "좋아요 수", example = "11")
        int likeCount,
        @Schema(description = "댓글 수", example = "10")
        int commentCount,
        @Schema(description = "북마크 수", example = "4")
        int bookmarkCount,
        @Schema(description = "첨부파일 목록")
        List<NoticeAttachment> attachments,
        @Schema(description = "읽음 여부", example = "true")
        boolean isRead,
        @Schema(description = "내 좋아요 여부", example = "true")
        boolean isLiked,
        @Schema(description = "내 북마크 여부", example = "true")
        boolean isBookmarked
) {
}
