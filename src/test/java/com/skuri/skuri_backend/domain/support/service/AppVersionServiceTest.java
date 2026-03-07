package com.skuri.skuri_backend.domain.support.service;

import com.skuri.skuri_backend.domain.support.dto.response.AppVersionResponse;
import com.skuri.skuri_backend.domain.support.entity.AppVersion;
import com.skuri.skuri_backend.domain.support.repository.AppVersionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppVersionServiceTest {

    @Mock
    private AppVersionRepository appVersionRepository;

    @InjectMocks
    private AppVersionService appVersionService;

    @Test
    void getAppVersion_저장된데이터없을때기본값반환() {
        when(appVersionRepository.findById("ios")).thenReturn(Optional.empty());

        AppVersionResponse response = appVersionService.getAppVersion("ios");

        assertEquals("ios", response.platform());
        assertEquals("1.0.0", response.minimumVersion());
        assertFalse(response.forceUpdate());
        assertFalse(response.showButton());
        assertNull(response.message());
        assertNull(response.title());
        assertNull(response.buttonText());
        assertNull(response.buttonUrl());
    }

    @Test
    void getAppVersion_저장된데이터가있으면저장값반환() {
        when(appVersionRepository.findById("android")).thenReturn(Optional.of(AppVersion.create(
                "android",
                "1.6.0",
                true,
                "안정성 개선 버전입니다.",
                "업데이트 안내",
                true,
                "업데이트",
                "https://play.google.com/store/apps/details?id=com.skuri"
        )));

        AppVersionResponse response = appVersionService.getAppVersion("android");

        assertEquals("android", response.platform());
        assertEquals("1.6.0", response.minimumVersion());
        assertEquals(true, response.forceUpdate());
        assertEquals("업데이트", response.buttonText());
    }
}
