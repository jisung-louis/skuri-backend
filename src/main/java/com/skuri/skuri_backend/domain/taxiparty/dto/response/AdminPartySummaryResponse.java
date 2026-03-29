package com.skuri.skuri_backend.domain.taxiparty.dto.response;

import com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "관리자 파티 목록 아이템 응답")
public record AdminPartySummaryResponse(
        @Schema(description = "파티 ID", example = "party-20260304-001")
        String id,
        @Schema(description = "파티 상태", example = "OPEN")
        PartyStatus status,
        @Schema(description = "리더 ID", example = "leader-uid")
        String leaderId,
        @Schema(description = "리더 닉네임", example = "스쿠리 유저", nullable = true)
        String leaderNickname,
        @Schema(description = "출발지/목적지 요약", example = "성결대학교 -> 안양역")
        String routeSummary,
        @Schema(description = "출발 시각", example = "2026-03-04T21:00:00")
        LocalDateTime departureTime,
        @Schema(description = "현재 인원", example = "2")
        int currentMembers,
        @Schema(description = "최대 인원", example = "4")
        int maxMembers,
        @Schema(description = "생성 시각", example = "2026-03-04T19:00:00")
        LocalDateTime createdAt
) {
}
