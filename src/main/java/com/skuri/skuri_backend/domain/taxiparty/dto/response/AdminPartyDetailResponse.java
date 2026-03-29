package com.skuri.skuri_backend.domain.taxiparty.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyEndReason;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus;
import com.skuri.skuri_backend.domain.taxiparty.entity.SettlementStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "관리자 파티 상세 응답")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AdminPartyDetailResponse(
        @Schema(description = "파티 ID", example = "party-20260304-001")
        String id,
        @Schema(description = "파티 상태", example = "ARRIVED")
        PartyStatus status,
        @Schema(description = "종료 사유", example = "FORCE_ENDED", nullable = true)
        PartyEndReason endReason,
        @Schema(description = "리더 ID", example = "leader-uid")
        String leaderId,
        @Schema(description = "리더 닉네임", example = "스쿠리 유저", nullable = true)
        String leaderNickname,
        @Schema(description = "리더 요약 정보", nullable = true)
        AdminPartyLeaderResponse leader,
        @Schema(description = "출발지/목적지 요약", example = "성결대학교 -> 안양역")
        String routeSummary,
        @Schema(description = "출발지")
        PartyLocationResponse departure,
        @Schema(description = "목적지")
        PartyLocationResponse destination,
        @Schema(description = "출발 시각", example = "2026-03-04T21:00:00")
        LocalDateTime departureTime,
        @Schema(description = "현재 인원", example = "3")
        int currentMembers,
        @Schema(description = "최대 인원", example = "4")
        int maxMembers,
        @Schema(description = "파티 멤버 목록")
        List<PartyMemberResponse> members,
        @Schema(description = "태그 목록", nullable = true)
        List<String> tags,
        @Schema(description = "상세 설명", example = "정문 앞 택시승강장 집합", nullable = true)
        String detail,
        @Schema(description = "대기 중인 동승 요청 수", example = "2")
        long pendingJoinRequestCount,
        @Schema(description = "정산 상태", example = "PENDING", nullable = true)
        SettlementStatus settlementStatus,
        @Schema(description = "정산 요약", nullable = true)
        SettlementSummaryResponse settlement,
        @Schema(description = "파티 채팅방 ID", example = "party:party-20260304-001", nullable = true)
        String chatRoomId,
        @Schema(description = "생성 시각", example = "2026-03-04T19:00:00")
        LocalDateTime createdAt,
        @Schema(description = "최근 수정 시각", example = "2026-03-04T20:00:00")
        LocalDateTime updatedAt,
        @Schema(description = "종료 시각", example = "2026-03-04T22:10:00", nullable = true)
        LocalDateTime endedAt
) {
}
