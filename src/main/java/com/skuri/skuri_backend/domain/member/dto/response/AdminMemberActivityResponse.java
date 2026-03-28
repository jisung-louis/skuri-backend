package com.skuri.skuri_backend.domain.member.dto.response;

import com.skuri.skuri_backend.domain.board.entity.PostCategory;
import com.skuri.skuri_backend.domain.support.entity.InquiryStatus;
import com.skuri.skuri_backend.domain.support.entity.InquiryType;
import com.skuri.skuri_backend.domain.support.entity.ReportStatus;
import com.skuri.skuri_backend.domain.support.entity.ReportTargetType;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "관리자 회원 활동 요약 응답")
public record AdminMemberActivityResponse(
        @Schema(description = "회원 ID(Firebase UID)", example = "dw9rPtuticbjnaYPkeiF3RGPpqk1")
        String memberId,
        @Schema(description = "요약 생성 시각", example = "2026-03-29T16:00:00")
        LocalDateTime generatedAt,
        @Schema(description = "활동 건수 요약")
        ActivityCounts counts,
        @Schema(description = "최근 게시글 목록", nullable = false)
        List<RecentPost> recentPosts,
        @Schema(description = "최근 댓글 목록", nullable = false)
        List<RecentComment> recentComments,
        @Schema(description = "최근 파티 목록", nullable = false)
        List<RecentParty> recentParties,
        @Schema(description = "최근 문의 목록", nullable = false)
        List<RecentInquiry> recentInquiries,
        @Schema(description = "최근 신고 목록", nullable = false)
        List<RecentReport> recentReports
) {

    @Schema(description = "활동 건수")
    public record ActivityCounts(
            @Schema(description = "현재 저장된 active 게시글 수", example = "12")
            long posts,
            @Schema(description = "현재 저장된 active 댓글 수", example = "34")
            long comments,
            @Schema(description = "리더로 생성한 파티 수", example = "3")
            long partiesCreated,
            @Schema(description = "리더를 제외한 파티 참여 수", example = "7")
            long partiesJoined,
            @Schema(description = "문의 등록 수", example = "2")
            long inquiries,
            @Schema(description = "신고 제출 수", example = "1")
            long reportsSubmitted
    ) {
    }

    @Schema(description = "최근 게시글")
    public record RecentPost(
            @Schema(description = "게시글 ID", example = "post-1")
            String id,
            @Schema(description = "게시글 제목", example = "택시 파티 구해요")
            String title,
            @Schema(description = "게시글 카테고리", example = "GENERAL")
            PostCategory category,
            @Schema(description = "작성 시각", example = "2026-03-28T14:00:00")
            LocalDateTime createdAt
    ) {
    }

    @Schema(description = "최근 댓글")
    public record RecentComment(
            @Schema(description = "댓글 ID", example = "comment-1")
            String id,
            @Schema(description = "게시글 ID", example = "post-1")
            String postId,
            @Schema(description = "게시글 제목", example = "택시 파티 구해요")
            String postTitle,
            @Schema(description = "댓글 내용 미리보기", example = "저도 참여하고 싶어요")
            String contentPreview,
            @Schema(description = "작성 시각", example = "2026-03-28T14:10:00")
            LocalDateTime createdAt
    ) {
    }

    @Schema(description = "최근 파티")
    public record RecentParty(
            @Schema(description = "파티 ID", example = "party-1")
            String id,
            @Schema(description = "회원 역할", example = "LEADER", allowableValues = {"LEADER", "JOINED"})
            PartyRole role,
            @Schema(description = "파티 상태", example = "OPEN")
            PartyStatus status,
            @Schema(description = "출발지/도착지 요약", example = "성결대 정문 → 안양역")
            String routeSummary,
            @Schema(description = "출발 시각", example = "2026-03-30T18:00:00")
            LocalDateTime departureTime,
            @Schema(description = "생성 시각", example = "2026-03-29T09:00:00")
            LocalDateTime createdAt
    ) {
    }

    @Schema(description = "최근 문의")
    public record RecentInquiry(
            @Schema(description = "문의 ID", example = "inquiry-1")
            String id,
            @Schema(description = "문의 유형", example = "ACCOUNT")
            InquiryType type,
            @Schema(description = "문의 제목", example = "계정 문의")
            String subject,
            @Schema(description = "문의 상태", example = "PENDING")
            InquiryStatus status,
            @Schema(description = "생성 시각", example = "2026-03-28T11:00:00")
            LocalDateTime createdAt
    ) {
    }

    @Schema(description = "최근 신고")
    public record RecentReport(
            @Schema(description = "신고 ID", example = "report-1")
            String id,
            @Schema(description = "신고 대상 타입", example = "POST")
            ReportTargetType targetType,
            @Schema(description = "신고 대상 ID", example = "post-9")
            String targetId,
            @Schema(description = "신고 카테고리", example = "SPAM")
            String category,
            @Schema(description = "신고 상태", example = "REVIEWING")
            ReportStatus status,
            @Schema(description = "생성 시각", example = "2026-03-27T20:00:00")
            LocalDateTime createdAt
    ) {
    }

    public enum PartyRole {
        LEADER,
        JOINED
    }
}
