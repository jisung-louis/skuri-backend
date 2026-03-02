package com.skuri.skuri_backend.domain.member.dto.response;

public record MemberPublicProfileResponse(
        String id,
        String nickname,
        String department,
        String photoUrl
) {
}
