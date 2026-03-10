package com.skuri.skuri_backend.domain.notification.service;

import com.skuri.skuri_backend.domain.notification.entity.FcmToken;
import com.skuri.skuri_backend.domain.notification.repository.FcmTokenRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collection;

@Service
public class FcmTokenService {

    private final FcmTokenRepository fcmTokenRepository;
    private final TransactionTemplate transactionTemplate;

    public FcmTokenService(FcmTokenRepository fcmTokenRepository, PlatformTransactionManager transactionManager) {
        this.fcmTokenRepository = fcmTokenRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public void register(String memberId, String token, String platform) {
        String normalizedToken = normalizeToken(token);
        String normalizedPlatform = normalizePlatform(platform);

        try {
            transactionTemplate.executeWithoutResult(status -> upsert(memberId, normalizedToken, normalizedPlatform));
        } catch (DataIntegrityViolationException e) {
            transactionTemplate.executeWithoutResult(status -> {
                FcmToken existing = fcmTokenRepository.findByToken(normalizedToken)
                        .orElseThrow(() -> e);
                existing.registerTo(memberId, normalizedPlatform);
            });
        }
    }

    @Transactional
    public void delete(String memberId, String token) {
        String normalizedToken = normalizeToken(token);
        FcmToken existing = fcmTokenRepository.findByToken(normalizedToken).orElse(null);
        if (existing == null || !existing.belongsTo(memberId)) {
            return;
        }

        fcmTokenRepository.delete(existing);
    }

    @Transactional
    public void touchTokens(Collection<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return;
        }

        fcmTokenRepository.findByTokenIn(tokens)
                .forEach(FcmToken::touch);
    }

    @Transactional
    public void purgeTokens(Collection<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return;
        }

        fcmTokenRepository.deleteByTokenIn(tokens);
    }

    @Transactional
    public void deleteAllByUserId(String memberId) {
        fcmTokenRepository.deleteByUserId(memberId);
    }

    private String normalizeToken(String token) {
        return token == null ? "" : token.trim();
    }

    private String normalizePlatform(String platform) {
        return platform == null ? "android" : platform.trim().toLowerCase();
    }

    private void upsert(String memberId, String token, String platform) {
        FcmToken existing = fcmTokenRepository.findByToken(token).orElse(null);
        if (existing == null) {
            fcmTokenRepository.saveAndFlush(FcmToken.create(memberId, token, platform));
            return;
        }

        existing.registerTo(memberId, platform);
    }
}
