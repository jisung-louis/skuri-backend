package com.skuri.skuri_backend.domain.member.service;

import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationSettingBackfillServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private NotificationSettingBackfillService notificationSettingBackfillService;

    @Test
    void backfillAcademicScheduleDefaults_기존회원null설정을기본값으로채운다() {
        Member legacyMember = Member.create("member-1", "member-1@sungkyul.ac.kr", "사용자", LocalDateTime.now());
        ReflectionTestUtils.setField(legacyMember.getNotificationSetting(), "academicScheduleNotifications", null);
        ReflectionTestUtils.setField(legacyMember.getNotificationSetting(), "academicScheduleDayBeforeEnabled", null);
        ReflectionTestUtils.setField(legacyMember.getNotificationSetting(), "academicScheduleAllEventsEnabled", null);

        when(memberRepository.findAll()).thenReturn(List.of(legacyMember));

        notificationSettingBackfillService.backfillAcademicScheduleDefaults();

        assertTrue(legacyMember.getNotificationSetting().isAcademicScheduleNotifications());
        assertTrue(legacyMember.getNotificationSetting().isAcademicScheduleDayBeforeEnabled());
        assertFalse(legacyMember.getNotificationSetting().isAcademicScheduleAllEventsEnabled());
    }
}
