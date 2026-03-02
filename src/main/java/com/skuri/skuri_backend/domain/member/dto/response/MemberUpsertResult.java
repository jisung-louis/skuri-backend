package com.skuri.skuri_backend.domain.member.dto.response;

public record MemberUpsertResult(
        boolean created,
        MemberCreateResponse member
) {

    public static MemberUpsertResult created(MemberCreateResponse member) {
        return new MemberUpsertResult(true, member);
    }

    public static MemberUpsertResult existing(MemberCreateResponse member) {
        return new MemberUpsertResult(false, member);
    }
}
