package com.skuri.skuri_backend.domain.notification.repository;

import com.skuri.skuri_backend.domain.notification.entity.UserNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserNotificationRepository extends JpaRepository<UserNotification, String> {

    Page<UserNotification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    Page<UserNotification> findByUserIdAndReadFalseOrderByCreatedAtDesc(String userId, Pageable pageable);

    long countByUserIdAndReadFalse(String userId);

    List<UserNotification> findByUserIdAndReadFalseOrderByCreatedAtDesc(String userId);

    List<UserNotification> findByUserIdOrderByCreatedAtDesc(String userId);
}
