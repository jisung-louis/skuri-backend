package com.skuri.skuri_backend.domain.taxiparty.dto.response;

import com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "파티 상세 응답")
public record PartyDetailResponse(
        @Schema(description = "파티 ID", example = "party_uuid")
        String id,
        @Schema(description = "리더 ID", example = "leader_uuid")
        String leaderId,
        @Schema(description = "리더 닉네임", example = "홍길동", nullable = true)
        String leaderName,
        @Schema(description = "리더 프로필 이미지", nullable = true)
        String leaderPhotoUrl,
        @Schema(description = "출발지")
        PartyLocationResponse departure,
        @Schema(description = "목적지")
        PartyLocationResponse destination,
        @Schema(description = "출발 시각", example = "2026-03-03T14:00:00")
        LocalDateTime departureTime,
        @Schema(description = "최대 인원", example = "4")
        int maxMembers,
        @Schema(description = "멤버 목록")
        List<PartyMemberResponse> members,
        @Schema(description = "태그 목록", nullable = true)
        List<String> tags,
        @Schema(description = "상세 설명", nullable = true)
        String detail,
        @Schema(description = "파티 상태", example = "OPEN")
        PartyStatus status,
        @Schema(description = "정산 정보", nullable = true)
        SettlementSummaryResponse settlement,
        @Schema(description = "생성 시각", example = "2026-03-03T12:00:00")
        LocalDateTime createdAt
) {
}
