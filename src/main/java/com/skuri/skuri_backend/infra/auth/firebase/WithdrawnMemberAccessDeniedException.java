package com.skuri.skuri_backend.infra.auth.firebase;

import org.springframework.security.access.AccessDeniedException;

public class WithdrawnMemberAccessDeniedException extends AccessDeniedException {

    public WithdrawnMemberAccessDeniedException() {
        super("탈퇴한 회원은 서비스에 접근할 수 없습니다.");
    }
}
