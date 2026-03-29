package com.skuri.skuri_backend.domain.board.dto.response;

import com.skuri.skuri_backend.domain.board.constant.BoardModerationStatus;
import com.skuri.skuri_backend.domain.board.entity.PostCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "관리자 게시글 상세 응답")
public record AdminPostDetailResponse(
        @Schema(description = "게시글 ID", example = "post_uuid")
        String id,
        @Schema(description = "카테고리", example = "GENERAL")
        PostCategory category,
        @Schema(description = "제목", example = "게시글 제목")
        String title,
        @Schema(description = "본문", example = "게시글 전체 내용")
        String content,
        @Schema(description = "작성자 ID", nullable = true, example = "member-1")
        String authorId,
        @Schema(description = "작성자 닉네임", nullable = true, example = "스쿠리유저")
        String authorNickname,
        @Schema(description = "작성자 실명", nullable = true, example = "홍길동")
        String authorRealname,
        @Schema(description = "익명 글 여부", example = "false")
        boolean isAnonymous,
        @Schema(description = "조회수", example = "42")
        int viewCount,
        @Schema(description = "좋아요 수", example = "10")
        int likeCount,
        @Schema(description = "댓글 수", example = "5")
        int commentCount,
        @Schema(description = "북마크 수", example = "3")
        int bookmarkCount,
        @Schema(description = "생성 시각", example = "2026-03-29T12:00:00")
        LocalDateTime createdAt,
        @Schema(description = "수정 시각", example = "2026-03-29T12:30:00")
        LocalDateTime updatedAt,
        @Schema(description = "관리자 moderation 상태", example = "HIDDEN")
        BoardModerationStatus moderationStatus,
        @Schema(description = "대표 썸네일 URL", nullable = true, example = "https://cdn.skuri.app/posts/post-1/image-1-thumb.jpg")
        String thumbnailUrl,
        @Schema(description = "이미지 목록")
        List<PostImageResponse> images
) {
}
