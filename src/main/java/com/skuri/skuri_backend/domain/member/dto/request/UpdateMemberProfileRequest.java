package com.skuri.skuri_backend.domain.member.dto.request;

import com.skuri.skuri_backend.domain.member.validation.ValidDepartment;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "내 프로필 수정 요청")
public record UpdateMemberProfileRequest(
        @Schema(description = "앱 내 닉네임", example = "스쿠리 유저", nullable = true)
        @Size(max = 50, message = "nickname은 50자 이하여야 합니다.")
        String nickname,

        @Schema(description = "학번", example = "2023112233", nullable = true)
        @Size(max = 20, message = "studentId는 20자 이하여야 합니다.")
        String studentId,

        @Schema(description = "학과", example = "컴퓨터공학과", nullable = true)
        @Size(max = 50, message = "department는 50자 이하여야 합니다.")
        @ValidDepartment
        String department,

        @Schema(description = "앱 내 프로필 이미지 URL", example = "https://cdn.skuri.app/profiles/user-1.png", nullable = true)
        @Size(max = 500, message = "photoUrl은 500자 이하여야 합니다.")
        String photoUrl
) {
}
