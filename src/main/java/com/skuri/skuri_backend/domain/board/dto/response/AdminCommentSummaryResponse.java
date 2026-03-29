package com.skuri.skuri_backend.domain.board.dto.response;

import com.skuri.skuri_backend.domain.board.constant.BoardModerationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "관리자 댓글 목록 아이템 응답")
public record AdminCommentSummaryResponse(
        @Schema(description = "댓글 ID", example = "comment_uuid")
        String id,
        @Schema(description = "게시글 ID", example = "post_uuid")
        String postId,
        @Schema(description = "게시글 제목", nullable = true, example = "게시글 제목")
        String postTitle,
        @Schema(description = "작성자 ID", nullable = true, example = "member-1")
        String authorId,
        @Schema(description = "작성자 닉네임", nullable = true, example = "스쿠리유저")
        String authorNickname,
        @Schema(description = "작성자 실명", nullable = true, example = "홍길동")
        String authorRealname,
        @Schema(description = "댓글 내용 미리보기", example = "댓글 내용 일부...")
        String contentPreview,
        @Schema(description = "부모 댓글 ID", nullable = true, example = "parent_comment_uuid")
        String parentCommentId,
        @Schema(description = "생성 시각", example = "2026-03-29T12:00:00")
        LocalDateTime createdAt,
        @Schema(description = "관리자 moderation 상태", example = "VISIBLE")
        BoardModerationStatus moderationStatus
) {
}
