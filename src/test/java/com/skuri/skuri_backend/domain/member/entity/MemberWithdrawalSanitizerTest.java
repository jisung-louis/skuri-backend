package com.skuri.skuri_backend.domain.member.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemberWithdrawalSanitizerTest {

    @Test
    void redactEmail_구분기호나대소문자가달라도_충돌없이생성한다() {
        String first = MemberWithdrawalSanitizer.redactEmail("member-1");
        String second = MemberWithdrawalSanitizer.redactEmail("Member1");

        assertNotEquals(first, second);
        assertTrue(first.endsWith("@deleted.skuri.local"));
        assertTrue(second.endsWith("@deleted.skuri.local"));
    }
}
