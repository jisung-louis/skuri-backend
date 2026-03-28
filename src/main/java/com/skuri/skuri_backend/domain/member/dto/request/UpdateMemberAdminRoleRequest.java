package com.skuri.skuri_backend.domain.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "관리자 권한 변경 요청")
public record UpdateMemberAdminRoleRequest(
        @NotNull
        @Schema(description = "관리자 권한 여부", example = "true")
        Boolean isAdmin
) {
}
