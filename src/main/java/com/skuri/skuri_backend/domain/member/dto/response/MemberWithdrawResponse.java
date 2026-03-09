package com.skuri.skuri_backend.domain.member.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원 탈퇴 응답")
public record MemberWithdrawResponse(
        @Schema(description = "처리 결과 메시지", example = "회원 탈퇴가 완료되었습니다.")
        String message
) {
}
