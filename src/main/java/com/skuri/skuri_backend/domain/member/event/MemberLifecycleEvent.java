package com.skuri.skuri_backend.domain.member.event;

public sealed interface MemberLifecycleEvent permits MemberLifecycleEvent.MemberWithdrawn {

    record MemberWithdrawn(String memberId) implements MemberLifecycleEvent {
    }
}
