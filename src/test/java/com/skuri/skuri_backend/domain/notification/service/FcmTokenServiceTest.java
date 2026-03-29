package com.skuri.skuri_backend.domain.notification.service;

import com.skuri.skuri_backend.domain.notification.entity.FcmToken;
import com.skuri.skuri_backend.domain.notification.repository.FcmTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FcmTokenServiceTest {

    @Mock
    private FcmTokenRepository fcmTokenRepository;

    @Mock
    private PlatformTransactionManager transactionManager;

    private FcmTokenService fcmTokenService;

    @BeforeEach
    void setUp() {
        lenient().when(transactionManager.getTransaction(any()))
                .thenReturn(new SimpleTransactionStatus(), new SimpleTransactionStatus());
        fcmTokenService = new FcmTokenService(fcmTokenRepository, transactionManager);
    }

    @Test
    void register_신규토큰이면_정규화후저장한다() {
        when(fcmTokenRepository.findByToken("device-token")).thenReturn(Optional.empty());

        fcmTokenService.register("member-1", " device-token ", "IOS", " 1.4.2 ");

        verify(fcmTokenRepository).saveAndFlush(argThat(token ->
                "member-1".equals(token.getUserId())
                        && "device-token".equals(token.getToken())
                        && "ios".equals(token.getPlatform())
                        && "1.4.2".equals(token.getAppVersion())
        ));
    }

    @Test
    void register_중복키충돌이면_재조회후기존토큰과앱버전을갱신한다() {
        FcmToken existing = FcmToken.create("member-old", "device-token", "android", "1.0.0");
        ReflectionTestUtils.setField(existing, "id", 1L);

        when(fcmTokenRepository.findByToken("device-token"))
                .thenReturn(Optional.empty(), Optional.of(existing));
        when(fcmTokenRepository.saveAndFlush(any(FcmToken.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate token"));

        fcmTokenService.register("member-1", "device-token", "ios", "1.4.2");

        verify(fcmTokenRepository, times(2)).findByToken("device-token");
        assertEquals("member-1", existing.getUserId());
        assertEquals("ios", existing.getPlatform());
        assertEquals("1.4.2", existing.getAppVersion());
    }

    @Test
    void register_기존토큰재등록시_appVersion이null이면_기존값을유지한다() {
        FcmToken existing = FcmToken.create("member-1", "device-token", "android", "1.4.2");
        when(fcmTokenRepository.findByToken("device-token")).thenReturn(Optional.of(existing));

        fcmTokenService.register("member-1", "device-token", "ios", " ");

        assertEquals("member-1", existing.getUserId());
        assertEquals("ios", existing.getPlatform());
        assertEquals("1.4.2", existing.getAppVersion());
        verify(fcmTokenRepository, never()).saveAndFlush(any(FcmToken.class));
    }

    @Test
    void deleteAllByUserId_회원탈퇴시_모든FCM토큰을삭제한다() {
        fcmTokenService.deleteAllByUserId("member-1");

        verify(fcmTokenRepository).deleteByUserId("member-1");
    }
}
