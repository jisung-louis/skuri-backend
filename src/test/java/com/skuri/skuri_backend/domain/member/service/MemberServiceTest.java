package com.skuri.skuri_backend.domain.member.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.member.dto.request.UpdateMemberBankAccountRequest;
import com.skuri.skuri_backend.domain.member.dto.request.UpdateMemberNotificationSettingsRequest;
import com.skuri.skuri_backend.domain.member.dto.request.UpdateMemberProfileRequest;
import com.skuri.skuri_backend.domain.member.dto.response.MemberMeResponse;
import com.skuri.skuri_backend.domain.member.dto.response.MemberPublicProfileResponse;
import com.skuri.skuri_backend.domain.member.dto.response.MemberUpsertResult;
import com.skuri.skuri_backend.domain.member.entity.LinkedAccount;
import com.skuri.skuri_backend.domain.member.entity.LinkedAccountProvider;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.exception.MemberNotFoundException;
import com.skuri.skuri_backend.domain.member.repository.LinkedAccountRepository;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMember;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private LinkedAccountRepository linkedAccountRepository;

    @InjectMocks
    private MemberService memberService;

    @Test
    void createMember_신규회원_생성성공() {
        AuthenticatedMember authenticatedMember = authenticatedMember();
        when(linkedAccountRepository.saveAndFlush(any(LinkedAccount.class))).thenReturn(null);

        MemberUpsertResult result = memberService.createMember(authenticatedMember);

        assertTrue(result.created());
        assertEquals(authenticatedMember.uid(), result.member().id());
    }

    @Test
    void createMember_저장충돌시_기존회원으로복구() {
        AuthenticatedMember authenticatedMember = authenticatedMember();
        Member existingMember = memberEntity(authenticatedMember.uid(), authenticatedMember.email());

        doThrow(new DataIntegrityViolationException("duplicate member"))
                .when(memberRepository).insert(any(Member.class));
        when(memberRepository.findById(authenticatedMember.uid())).thenReturn(Optional.of(existingMember));
        when(linkedAccountRepository.saveAndFlush(any(LinkedAccount.class))).thenReturn(null);

        MemberUpsertResult result = memberService.createMember(authenticatedMember);

        assertFalse(result.created());
        assertEquals(authenticatedMember.uid(), result.member().id());
    }

    @Test
    void createMember_저장충돌후_재조회실패시_충돌예외() {
        AuthenticatedMember authenticatedMember = authenticatedMember();

        doThrow(new DataIntegrityViolationException("duplicate member"))
                .when(memberRepository).insert(any(Member.class));
        when(memberRepository.findById(authenticatedMember.uid())).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> memberService.createMember(authenticatedMember)
        );

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        verify(linkedAccountRepository, never()).saveAndFlush(any(LinkedAccount.class));
    }

    @Test
    void createMember_linkedAccount중복충돌은_무해처리() {
        AuthenticatedMember authenticatedMember = authenticatedMember();

        when(linkedAccountRepository.saveAndFlush(any(LinkedAccount.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate linked account"));
        when(linkedAccountRepository.existsByMemberIdAndProvider(authenticatedMember.uid(), LinkedAccountProvider.GOOGLE))
                .thenReturn(true);

        MemberUpsertResult result = memberService.createMember(authenticatedMember);

        assertTrue(result.created());
        verify(linkedAccountRepository).existsByMemberIdAndProvider(eq(authenticatedMember.uid()), eq(LinkedAccountProvider.GOOGLE));
    }

    @Test
    void createMember_provider이름이있어도_기본닉네임으로생성() {
        AuthenticatedMember authenticatedMember = new AuthenticatedMember(
                "firebase-uid",
                "user@sungkyul.ac.kr",
                "google.com",
                "google-provider-id",
                "구글표시이름",
                "https://example.com/profile.jpg"
        );
        when(linkedAccountRepository.saveAndFlush(any(LinkedAccount.class))).thenReturn(null);

        MemberUpsertResult result = memberService.createMember(authenticatedMember);

        assertTrue(result.created());
        assertEquals("스쿠리 유저", result.member().nickname());
        assertEquals("구글표시이름", result.member().realname());
        assertNull(result.member().photoUrl());
        verify(linkedAccountRepository).saveAndFlush(
                argThat(linkedAccount -> "https://example.com/profile.jpg".equals(linkedAccount.getPhotoUrl()))
        );
    }

    @Test
    void createMember_비소셜로그인_password인경우_linkedAccount부가필드null로저장() {
        AuthenticatedMember authenticatedMember = new AuthenticatedMember(
                "firebase-uid",
                "admin@sungkyul.ac.kr",
                "password",
                "password-provider-id",
                "관리자",
                "https://example.com/admin.jpg"
        );
        when(linkedAccountRepository.saveAndFlush(any(LinkedAccount.class))).thenReturn(null);

        MemberUpsertResult result = memberService.createMember(authenticatedMember);

        assertTrue(result.created());
        verify(linkedAccountRepository).saveAndFlush(
                argThat(linkedAccount ->
                        linkedAccount.getProvider() == LinkedAccountProvider.PASSWORD
                                && linkedAccount.getProviderId() == null
                                && linkedAccount.getEmail() == null
                                && linkedAccount.getProviderDisplayName() == null
                                && linkedAccount.getPhotoUrl() == null
                )
        );
    }

    @Test
    void updateMyProfile_부분수정_null필드유지() {
        Member member = Member.create("firebase-uid", "user@sungkyul.ac.kr", "기존실명", LocalDateTime.now());
        member.updateProfile("기존닉네임", "20201234", "컴퓨터공학과", "https://example.com/old.jpg");
        when(memberRepository.findById("firebase-uid")).thenReturn(Optional.of(member));

        MemberMeResponse response = memberService.updateMyProfile(
                "firebase-uid",
                new UpdateMemberProfileRequest("새닉네임", null, null, "https://example.com/new.jpg")
        );

        assertEquals("새닉네임", response.nickname());
        assertEquals("20201234", response.studentId());
        assertEquals("컴퓨터공학과", response.department());
        assertEquals("기존실명", response.realname());
        assertEquals("https://example.com/new.jpg", response.photoUrl());
    }

    @Test
    void updateMyProfile_회원없음_MEMBER_NOT_FOUND() {
        when(memberRepository.findById("not-found")).thenReturn(Optional.empty());

        assertThrows(
                MemberNotFoundException.class,
                () -> memberService.updateMyProfile("not-found", new UpdateMemberProfileRequest(null, null, null, null))
        );
    }

    @Test
    void updateMyBankAccount_hideNameNull이면False() {
        Member member = memberEntity("firebase-uid", "user@sungkyul.ac.kr");
        when(memberRepository.findById("firebase-uid")).thenReturn(Optional.of(member));

        MemberMeResponse response = memberService.updateMyBankAccount(
                "firebase-uid",
                new UpdateMemberBankAccountRequest("카카오뱅크", "3333-01-1234567", "홍길동", null)
        );

        assertNotNull(response.bankAccount());
        assertEquals("카카오뱅크", response.bankAccount().bankName());
        assertEquals("3333-01-1234567", response.bankAccount().accountNumber());
        assertEquals("홍길동", response.bankAccount().accountHolder());
        assertFalse(response.bankAccount().hideName());
    }

    @Test
    void updateMyBankAccount_회원없음_MEMBER_NOT_FOUND() {
        when(memberRepository.findById("not-found")).thenReturn(Optional.empty());

        assertThrows(
                MemberNotFoundException.class,
                () -> memberService.updateMyBankAccount(
                        "not-found",
                        new UpdateMemberBankAccountRequest("카카오뱅크", "3333-01-1234567", "홍길동", false)
                )
        );
    }

    @Test
    void updateMyNotificationSettings_부분수정_지정필드만변경() {
        Member member = memberEntity("firebase-uid", "user@sungkyul.ac.kr");
        when(memberRepository.findById("firebase-uid")).thenReturn(Optional.of(member));

        MemberMeResponse response = memberService.updateMyNotificationSettings(
                "firebase-uid",
                new UpdateMemberNotificationSettingsRequest(
                        null,
                        false,
                        null,
                        null,
                        false,
                        true,
                        null,
                        null,
                        null,
                        null,
                        Map.of("news", false, "academy", true, "scholarship", true)
                )
        );

        assertNotNull(response.notificationSetting());
        assertTrue(response.notificationSetting().allNotifications());
        assertFalse(response.notificationSetting().partyNotifications());
        assertTrue(response.notificationSetting().noticeNotifications());
        assertTrue(response.notificationSetting().boardLikeNotifications());
        assertFalse(response.notificationSetting().commentNotifications());
        assertTrue(response.notificationSetting().bookmarkedPostCommentNotifications());
        assertTrue(response.notificationSetting().systemNotifications());
        assertTrue(response.notificationSetting().academicScheduleNotifications());
        assertTrue(response.notificationSetting().academicScheduleDayBeforeEnabled());
        assertFalse(response.notificationSetting().academicScheduleAllEventsEnabled());
        assertEquals(Map.of("news", false, "academy", true, "scholarship", true), response.notificationSetting().noticeNotificationsDetail());
    }

    @Test
    void updateMyNotificationSettings_회원없음_MEMBER_NOT_FOUND() {
        when(memberRepository.findById("not-found")).thenReturn(Optional.empty());

        assertThrows(
                MemberNotFoundException.class,
                () -> memberService.updateMyNotificationSettings(
                        "not-found",
                        new UpdateMemberNotificationSettingsRequest(
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null
                        )
                )
        );
    }

    @Test
    void getMyProfile_lastLogin갱신() {
        Member member = memberEntity("firebase-uid", "user@sungkyul.ac.kr");
        LocalDateTime oldLastLogin = LocalDateTime.now().minusDays(1);
        member.updateLastLogin(oldLastLogin);
        when(memberRepository.findById("firebase-uid")).thenReturn(Optional.of(member));

        LocalDateTime callStartedAt = LocalDateTime.now();
        MemberMeResponse response = memberService.getMyProfile("firebase-uid");

        assertNotNull(response.lastLogin());
        assertTrue(response.lastLogin().isAfter(oldLastLogin));
        assertTrue(!response.lastLogin().isBefore(callStartedAt));
    }

    @Test
    void getMyProfile_기존회원의학사일정알림기본값을보존한다() {
        Member member = memberEntity("firebase-uid", "user@sungkyul.ac.kr");
        ReflectionTestUtils.setField(member.getNotificationSetting(), "academicScheduleNotifications", null);
        ReflectionTestUtils.setField(member.getNotificationSetting(), "academicScheduleDayBeforeEnabled", null);
        ReflectionTestUtils.setField(member.getNotificationSetting(), "academicScheduleAllEventsEnabled", null);
        when(memberRepository.findById("firebase-uid")).thenReturn(Optional.of(member));

        MemberMeResponse response = memberService.getMyProfile("firebase-uid");

        assertNotNull(response.notificationSetting());
        assertTrue(response.notificationSetting().academicScheduleNotifications());
        assertTrue(response.notificationSetting().academicScheduleDayBeforeEnabled());
        assertFalse(response.notificationSetting().academicScheduleAllEventsEnabled());
    }

    @Test
    void getMemberById_공개프로필반환() {
        Member member = memberEntity("firebase-uid", "user@sungkyul.ac.kr");
        member.updateProfile("공개닉네임", null, "컴퓨터공학과", "https://example.com/target.jpg");
        when(memberRepository.findById("firebase-uid")).thenReturn(Optional.of(member));

        MemberPublicProfileResponse response = memberService.getMemberById("firebase-uid");

        assertEquals("firebase-uid", response.id());
        assertEquals("공개닉네임", response.nickname());
        assertEquals("컴퓨터공학과", response.department());
        assertEquals("https://example.com/target.jpg", response.photoUrl());
    }

    @Test
    void getMemberById_회원없음_MEMBER_NOT_FOUND() {
        when(memberRepository.findById("not-found")).thenReturn(Optional.empty());

        assertThrows(
                MemberNotFoundException.class,
                () -> memberService.getMemberById("not-found")
        );
    }

    private AuthenticatedMember authenticatedMember() {
        return new AuthenticatedMember(
                "firebase-uid",
                "user@sungkyul.ac.kr",
                "google.com",
                "google-provider-id",
                "홍길동",
                "https://example.com/profile.jpg"
        );
    }

    private Member memberEntity(String id, String email) {
        return Member.create(
                id,
                email,
                null,
                LocalDateTime.now()
        );
    }
}
