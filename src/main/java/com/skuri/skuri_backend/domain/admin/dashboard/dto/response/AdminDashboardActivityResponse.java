package com.skuri.skuri_backend.domain.admin.dashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "관리자 대시보드 활동 추이 응답")
public record AdminDashboardActivityResponse(
        @Schema(description = "요청한 일수 버킷", example = "7")
        int days,
        @Schema(description = "일자 버킷 기준 타임존", example = "Asia/Seoul")
        String timezone,
        @Schema(description = "오래된 날짜부터 정렬된 일별 시계열")
        List<ActivitySeriesItem> series
) {

    @Schema(description = "일별 운영 추이 버킷")
    public record ActivitySeriesItem(
            @Schema(description = "집계 일자(Asia/Seoul)", example = "2026-03-23")
            LocalDate date,
            @Schema(description = "해당 일 가입 회원 수(joinedAt 기준)", example = "4")
            long newMembers,
            @Schema(description = "해당 일 생성 문의 수", example = "2")
            long inquiriesCreated,
            @Schema(description = "해당 일 생성 신고 수", example = "1")
            long reportsCreated,
            @Schema(description = "해당 일 생성 파티 수", example = "6")
            long partiesCreated
    ) {
    }
}
