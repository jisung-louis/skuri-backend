package com.skuri.skuri_backend.domain.app.repository;

import com.skuri.skuri_backend.domain.app.entity.AppNoticeReadStatus;
import com.skuri.skuri_backend.domain.app.entity.AppNoticeReadStatusId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppNoticeReadStatusRepository extends JpaRepository<AppNoticeReadStatus, AppNoticeReadStatusId> {

    Optional<AppNoticeReadStatus> findById_UserIdAndId_AppNoticeId(String userId, String appNoticeId);

    long deleteById_UserId(String userId);

    long deleteById_AppNoticeId(String appNoticeId);
}
