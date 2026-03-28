package com.skuri.skuri_backend.domain.member.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.member.dto.request.UpdateMemberAdminRoleRequest;
import com.skuri.skuri_backend.domain.member.dto.response.AdminMemberDetailResponse;
import com.skuri.skuri_backend.domain.member.dto.response.AdminMemberSummaryResponse;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.entity.MemberStatus;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberAdminServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberAdminService memberAdminService;

    @Test
    void getAdminMembers_필터와정렬을반영한페이지를반환한다() {
        Member member = member("member-1");
        member.updateProfile("스쿠리 유저", "2023112233", "컴퓨터공학과", null);
        when(memberRepository.searchAdminMembers(
                eq("홍길동"),
                eq(MemberStatus.ACTIVE),
                eq(false),
                eq("컴퓨터공학과"),
                any()
        )).thenReturn(new PageImpl<>(List.of(member)));

        var response = memberAdminService.getAdminMembers(
                "홍길동",
                MemberStatus.ACTIVE,
                false,
                "컴퓨터공학과",
                0,
                20
        );

        assertEquals(1, response.getContent().size());
        AdminMemberSummaryResponse content = response.getContent().getFirst();
        assertEquals("member-1", content.id());
        assertEquals("컴퓨터공학과", content.department());
        verify(memberRepository).searchAdminMembers(
                eq("홍길동"),
                eq(MemberStatus.ACTIVE),
                eq(false),
                eq("컴퓨터공학과"),
                argThat(pageable ->
                        pageable.getPageNumber() == 0
                                && pageable.getPageSize() == 20
                                && pageable.getSort().getOrderFor("joinedAt") != null
                                && pageable.getSort().getOrderFor("joinedAt").isDescending()
                )
        );
    }

    @Test
    void getAdminMember_탈퇴회원도상세조회할수있다() {
        Member withdrawnMember = member("withdrawn-member");
        withdrawnMember.withdraw(LocalDateTime.of(2026, 3, 29, 10, 15));
        when(memberRepository.findById("withdrawn-member")).thenReturn(Optional.of(withdrawnMember));

        AdminMemberDetailResponse response = memberAdminService.getAdminMember("withdrawn-member");

        assertEquals("withdrawn-member", response.id());
        assertEquals(MemberStatus.WITHDRAWN, response.status());
        assertNotNull(response.withdrawnAt());
        assertNotNull(response.notificationSetting());
    }

    @Test
    void updateAdminRole_성공() {
        Member member = member("member-1");
        when(memberRepository.findByIdForUpdate("member-1")).thenReturn(Optional.of(member));
        when(memberRepository.saveAndFlush(member)).thenReturn(member);

        AdminMemberDetailResponse response = memberAdminService.updateAdminRole(
                "admin-uid",
                "member-1",
                new UpdateMemberAdminRoleRequest(true)
        );

        assertEquals("member-1", response.id());
        assertEquals(true, response.isAdmin());
        verify(memberRepository).saveAndFlush(member);
    }

    @Test
    void updateAdminRole_자기자신이면_SELF_ADMIN_ROLE_CHANGE_NOT_ALLOWED() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> memberAdminService.updateAdminRole("admin-uid", "admin-uid", new UpdateMemberAdminRoleRequest(false))
        );

        assertEquals(ErrorCode.SELF_ADMIN_ROLE_CHANGE_NOT_ALLOWED, exception.getErrorCode());
        verify(memberRepository, never()).findByIdForUpdate(any());
        verify(memberRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateAdminRole_탈퇴회원이면_CONFLICT() {
        Member withdrawnMember = member("withdrawn-member");
        withdrawnMember.withdraw(LocalDateTime.now());
        when(memberRepository.findByIdForUpdate("withdrawn-member")).thenReturn(Optional.of(withdrawnMember));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> memberAdminService.updateAdminRole("admin-uid", "withdrawn-member", new UpdateMemberAdminRoleRequest(false))
        );

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        assertEquals("탈퇴한 회원의 관리자 권한은 변경할 수 없습니다.", exception.getMessage());
        verify(memberRepository, never()).saveAndFlush(any());
    }

    @Test
    void getAdminMembers_지원하지않는학과면_VALIDATION_ERROR() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> memberAdminService.getAdminMembers(null, null, null, "없는학과", 0, 20)
        );

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
    }

    private Member member(String id) {
        Member member = Member.create(id, id + "@sungkyul.ac.kr", "홍길동", LocalDateTime.of(2025, 3, 1, 9, 0));
        ReflectionTestUtils.setField(member, "lastLogin", LocalDateTime.of(2026, 3, 29, 10, 5));
        return member;
    }
}
