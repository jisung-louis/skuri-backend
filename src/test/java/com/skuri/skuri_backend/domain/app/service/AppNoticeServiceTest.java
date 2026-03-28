package com.skuri.skuri_backend.domain.app.service;

import com.skuri.skuri_backend.common.event.AfterCommitApplicationEventPublisher;
import com.skuri.skuri_backend.domain.app.dto.response.AppNoticeReadResponse;
import com.skuri.skuri_backend.domain.app.dto.response.AppNoticeUnreadCountResponse;
import com.skuri.skuri_backend.domain.app.entity.AppNotice;
import com.skuri.skuri_backend.domain.app.entity.AppNoticeCategory;
import com.skuri.skuri_backend.domain.app.entity.AppNoticePriority;
import com.skuri.skuri_backend.domain.app.entity.AppNoticeReadStatus;
import com.skuri.skuri_backend.domain.app.repository.AppNoticeReadStatusRepository;
import com.skuri.skuri_backend.domain.app.repository.AppNoticeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppNoticeServiceTest {

    @Mock
    private AppNoticeRepository appNoticeRepository;

    @Mock
    private AppNoticeReadStatusRepository appNoticeReadStatusRepository;

    @Mock
    private AfterCommitApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AppNoticeService appNoticeService;

    @Test
    void getUnreadCount_게시된공지기준개수를반환한다() {
        when(appNoticeRepository.countPublishedUnread(eq("member-1"), any(LocalDateTime.class))).thenReturn(3L);

        AppNoticeUnreadCountResponse response = appNoticeService.getUnreadCount("member-1");

        assertEquals(3, response.count());
    }

    @Test
    void markRead_기존읽음상태가있으면_기존readAt을그대로반환한다() {
        AppNotice appNotice = appNotice("app-notice-1");
        AppNoticeReadStatus existing = AppNoticeReadStatus.create(
                appNotice,
                "member-1",
                LocalDateTime.of(2026, 3, 20, 9, 30)
        );

        when(appNoticeRepository.findPublishedById(eq("app-notice-1"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(appNotice));
        when(appNoticeReadStatusRepository.findById_UserIdAndId_AppNoticeId("member-1", "app-notice-1"))
                .thenReturn(Optional.of(existing));

        AppNoticeReadResponse response = appNoticeService.markRead("member-1", "app-notice-1");

        assertEquals("app-notice-1", response.appNoticeId());
        assertEquals(LocalDateTime.of(2026, 3, 20, 9, 30), response.readAt());
        verify(appNoticeReadStatusRepository, never()).saveAndFlush(any(AppNoticeReadStatus.class));
    }

    @Test
    void markRead_기존상태없으면_새로생성한다() {
        AppNotice appNotice = appNotice("app-notice-1");

        when(appNoticeRepository.findPublishedById(eq("app-notice-1"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(appNotice));
        when(appNoticeReadStatusRepository.findById_UserIdAndId_AppNoticeId("member-1", "app-notice-1"))
                .thenReturn(Optional.empty());
        when(appNoticeReadStatusRepository.saveAndFlush(any(AppNoticeReadStatus.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AppNoticeReadResponse response = appNoticeService.markRead("member-1", "app-notice-1");

        assertEquals("app-notice-1", response.appNoticeId());
        assertNotNull(response.readAt());
    }

    @Test
    void markRead_동시중복읽음이면_기존상태로복구해성공한다() {
        AppNotice appNotice = appNotice("app-notice-1");
        AppNoticeReadStatus existing = AppNoticeReadStatus.create(
                appNotice,
                "member-1",
                LocalDateTime.of(2026, 3, 21, 8, 0)
        );

        when(appNoticeRepository.findPublishedById(eq("app-notice-1"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(appNotice));
        when(appNoticeReadStatusRepository.findById_UserIdAndId_AppNoticeId("member-1", "app-notice-1"))
                .thenReturn(Optional.empty(), Optional.of(existing));
        when(appNoticeReadStatusRepository.saveAndFlush(any(AppNoticeReadStatus.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        AppNoticeReadResponse response = appNoticeService.markRead("member-1", "app-notice-1");

        assertEquals(LocalDateTime.of(2026, 3, 21, 8, 0), response.readAt());
    }

    @Test
    void deleteAppNotice_읽음상태를먼저정리한다() {
        AppNotice appNotice = appNotice("app-notice-1");
        when(appNoticeRepository.findById("app-notice-1")).thenReturn(Optional.of(appNotice));

        appNoticeService.deleteAppNotice("app-notice-1");

        verify(appNoticeReadStatusRepository).deleteById_AppNoticeId("app-notice-1");
        verify(appNoticeRepository).delete(appNotice);
    }

    @Test
    void deleteAllReadStatusesByUserId_리포지토리에위임한다() {
        appNoticeService.deleteAllReadStatusesByUserId("member-1");

        verify(appNoticeReadStatusRepository).deleteById_UserId("member-1");
    }

    private AppNotice appNotice(String id) {
        AppNotice appNotice = AppNotice.create(
                "앱 공지",
                "내용",
                AppNoticeCategory.MAINTENANCE,
                AppNoticePriority.NORMAL,
                List.of(),
                null,
                LocalDateTime.of(2026, 3, 20, 0, 0)
        );
        ReflectionTestUtils.setField(appNotice, "id", id);
        return appNotice;
    }
}
