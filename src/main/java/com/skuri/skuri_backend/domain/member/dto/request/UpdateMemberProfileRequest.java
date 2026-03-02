package com.skuri.skuri_backend.domain.member.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateMemberProfileRequest(
        @Size(max = 50, message = "nickname은 50자 이하여야 합니다.")
        String nickname,

        @Size(max = 20, message = "studentId는 20자 이하여야 합니다.")
        String studentId,

        @Size(max = 50, message = "department는 50자 이하여야 합니다.")
        String department,

        @Size(max = 500, message = "photoUrl은 500자 이하여야 합니다.")
        String photoUrl
) {
}
