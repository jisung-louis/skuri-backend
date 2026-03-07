package com.skuri.skuri_backend.domain.notification.service;

import com.skuri.skuri_backend.domain.notification.entity.FcmToken;
import com.skuri.skuri_backend.domain.notification.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

@Service
@RequiredArgsConstructor
public class FcmTokenService {

    private final FcmTokenRepository fcmTokenRepository;

    @Transactional
    public void register(String memberId, String token, String platform) {
        String normalizedToken = normalizeToken(token);
        String normalizedPlatform = normalizePlatform(platform);

        FcmToken existing = fcmTokenRepository.findByToken(normalizedToken).orElse(null);
        if (existing == null) {
            fcmTokenRepository.save(FcmToken.create(memberId, normalizedToken, normalizedPlatform));
            return;
        }

        existing.registerTo(memberId, normalizedPlatform);
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

    private String normalizeToken(String token) {
        return token == null ? "" : token.trim();
    }

    private String normalizePlatform(String platform) {
        return platform == null ? "android" : platform.trim().toLowerCase();
    }
}
