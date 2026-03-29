package com.skuri.skuri_backend.domain.taxiparty.dto.request;

import com.skuri.skuri_backend.domain.taxiparty.constant.AdminPartyStatusAction;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "관리자 파티 상태 변경 요청")
public record UpdateAdminPartyStatusRequest(
        @NotNull(message = "action은 필수입니다.")
        @Schema(
                description = "관리자 상태 변경 액션",
                example = "CLOSE",
                allowableValues = {"CLOSE", "REOPEN", "CANCEL", "END"}
        )
        AdminPartyStatusAction action
) {
}
