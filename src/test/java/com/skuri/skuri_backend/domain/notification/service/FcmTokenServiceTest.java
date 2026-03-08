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
        when(transactionManager.getTransaction(any()))
                .thenReturn(new SimpleTransactionStatus(), new SimpleTransactionStatus());
        fcmTokenService = new FcmTokenService(fcmTokenRepository, transactionManager);
    }

    @Test
    void register_신규토큰이면_정규화후저장한다() {
        when(fcmTokenRepository.findByToken("device-token")).thenReturn(Optional.empty());

        fcmTokenService.register("member-1", " device-token ", "IOS");

        verify(fcmTokenRepository).saveAndFlush(argThat(token ->
                "member-1".equals(token.getUserId())
                        && "device-token".equals(token.getToken())
                        && "ios".equals(token.getPlatform())
        ));
    }

    @Test
    void register_중복키충돌이면_재조회후기존토큰을갱신한다() {
        FcmToken existing = FcmToken.create("member-old", "device-token", "android");
        ReflectionTestUtils.setField(existing, "id", 1L);

        when(fcmTokenRepository.findByToken("device-token"))
                .thenReturn(Optional.empty(), Optional.of(existing));
        when(fcmTokenRepository.saveAndFlush(any(FcmToken.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate token"));

        fcmTokenService.register("member-1", "device-token", "ios");

        verify(fcmTokenRepository, times(2)).findByToken("device-token");
        assertEquals("member-1", existing.getUserId());
        assertEquals("ios", existing.getPlatform());
    }
}
