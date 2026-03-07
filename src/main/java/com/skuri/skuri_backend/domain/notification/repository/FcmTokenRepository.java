package com.skuri.skuri_backend.domain.notification.repository;

import com.skuri.skuri_backend.domain.notification.entity.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    Optional<FcmToken> findByToken(String token);

    List<FcmToken> findByUserIdIn(Collection<String> userIds);

    List<FcmToken> findByTokenIn(Collection<String> tokens);

    @Transactional
    long deleteByUserIdAndToken(String userId, String token);

    @Transactional
    long deleteByTokenIn(Collection<String> tokens);
}
