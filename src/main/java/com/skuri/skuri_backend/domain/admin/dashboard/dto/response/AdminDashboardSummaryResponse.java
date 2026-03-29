package com.skuri.skuri_backend.domain.admin.dashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "관리자 대시보드 KPI 요약 응답")
public record AdminDashboardSummaryResponse(
        @Schema(description = "Asia/Seoul 기준 오늘 가입 회원 수", example = "12")
        long newMembersToday,
        @Schema(description = "전체 회원 수(members 전체 row 기준)", example = "4831")
        long totalMembers,
        @Schema(description = "관리자 권한 회원 수(isAdmin=true)", example = "4")
        long adminCount,
        @Schema(description = "현재 OPEN 상태 파티 수", example = "17")
        long openPartyCount,
        @Schema(description = "현재 PENDING 상태 문의 수", example = "9")
        long pendingInquiryCount,
        @Schema(description = "현재 PENDING 상태 신고 수", example = "3")
        long pendingReportCount,
        @Schema(description = "집계 생성 시각(Asia/Seoul)", example = "2026-03-29T18:00:00")
        LocalDateTime generatedAt
) {
}
