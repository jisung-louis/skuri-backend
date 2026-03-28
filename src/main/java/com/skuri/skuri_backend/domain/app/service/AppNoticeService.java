package com.skuri.skuri_backend.domain.app.service;

import com.skuri.skuri_backend.common.event.AfterCommitApplicationEventPublisher;
import com.skuri.skuri_backend.domain.app.dto.request.CreateAppNoticeRequest;
import com.skuri.skuri_backend.domain.app.dto.request.UpdateAppNoticeRequest;
import com.skuri.skuri_backend.domain.app.dto.response.AppNoticeCreateResponse;
import com.skuri.skuri_backend.domain.app.dto.response.AppNoticeReadResponse;
import com.skuri.skuri_backend.domain.app.dto.response.AppNoticeResponse;
import com.skuri.skuri_backend.domain.app.dto.response.AppNoticeUnreadCountResponse;
import com.skuri.skuri_backend.domain.app.entity.AppNotice;
import com.skuri.skuri_backend.domain.app.entity.AppNoticeReadStatus;
import com.skuri.skuri_backend.domain.app.exception.AppNoticeNotFoundException;
import com.skuri.skuri_backend.domain.app.repository.AppNoticeRepository;
import com.skuri.skuri_backend.domain.app.repository.AppNoticeReadStatusRepository;
import com.skuri.skuri_backend.domain.notification.event.NotificationDomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppNoticeService {

    private final AppNoticeRepository appNoticeRepository;
    private final AppNoticeReadStatusRepository appNoticeReadStatusRepository;
    private final AfterCommitApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<AppNoticeResponse> getPublishedNotices() {
        return appNoticeRepository.findPublished(LocalDateTime.now()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AppNoticeResponse getPublishedNotice(String appNoticeId) {
        return toResponse(findPublishedNoticeOrThrow(appNoticeId));
    }

    @Transactional(readOnly = true)
    public AppNoticeUnreadCountResponse getUnreadCount(String memberId) {
        return new AppNoticeUnreadCountResponse(appNoticeRepository.countPublishedUnread(memberId, LocalDateTime.now()));
    }

    @Transactional
    public AppNoticeReadResponse markRead(String memberId, String appNoticeId) {
        AppNotice appNotice = findPublishedNoticeOrThrow(appNoticeId);
        return appNoticeReadStatusRepository.findById_UserIdAndId_AppNoticeId(memberId, appNoticeId)
                .map(this::toReadResponse)
                .orElseGet(() -> createReadStatus(memberId, appNotice));
    }

    @Transactional
    public AppNoticeCreateResponse createAppNotice(CreateAppNoticeRequest request) {
        AppNotice appNotice = appNoticeRepository.save(AppNotice.create(
                request.title().trim(),
                request.content().trim(),
                request.category(),
                request.priority(),
                normalizeImageUrls(request.imageUrls()),
                trimToNull(request.actionUrl()),
                request.publishedAt()
        ));
        eventPublisher.publish(new NotificationDomainEvent.AppNoticeCreated(appNotice.getId()));
        return new AppNoticeCreateResponse(appNotice.getId(), appNotice.getTitle(), appNotice.getCreatedAt());
    }

    @Transactional
    public AppNoticeResponse updateAppNotice(String appNoticeId, UpdateAppNoticeRequest request) {
        AppNotice appNotice = appNoticeRepository.findById(appNoticeId)
                .orElseThrow(AppNoticeNotFoundException::new);
        appNotice.update(
                trimToNull(request.title()),
                trimToNull(request.content()),
                request.category(),
                request.priority(),
                request.imageUrls() == null ? null : normalizeImageUrls(request.imageUrls()),
                request.actionUrl() == null ? null : trimToNull(request.actionUrl()),
                request.publishedAt()
        );
        return toResponse(appNotice);
    }

    @Transactional
    public void deleteAppNotice(String appNoticeId) {
        AppNotice appNotice = appNoticeRepository.findById(appNoticeId)
                .orElseThrow(AppNoticeNotFoundException::new);
        appNoticeReadStatusRepository.deleteById_AppNoticeId(appNoticeId);
        appNoticeRepository.delete(appNotice);
    }

    @Transactional
    public void deleteAllReadStatusesByUserId(String memberId) {
        appNoticeReadStatusRepository.deleteById_UserId(memberId);
    }

    private AppNoticeResponse toResponse(AppNotice appNotice) {
        return new AppNoticeResponse(
                appNotice.getId(),
                appNotice.getTitle(),
                appNotice.getContent(),
                appNotice.getCategory(),
                appNotice.getPriority(),
                List.copyOf(appNotice.getImageUrls()),
                appNotice.getActionUrl(),
                appNotice.getPublishedAt(),
                appNotice.getCreatedAt(),
                appNotice.getUpdatedAt()
        );
    }

    private AppNoticeReadResponse toReadResponse(AppNoticeReadStatus status) {
        return new AppNoticeReadResponse(status.getId().getAppNoticeId(), true, status.getReadAt());
    }

    private AppNoticeReadResponse createReadStatus(String memberId, AppNotice appNotice) {
        LocalDateTime readAt = LocalDateTime.now();
        try {
            AppNoticeReadStatus saved = appNoticeReadStatusRepository.saveAndFlush(
                    AppNoticeReadStatus.create(appNotice, memberId, readAt)
            );
            return toReadResponse(saved);
        } catch (DataIntegrityViolationException e) {
            return appNoticeReadStatusRepository.findById_UserIdAndId_AppNoticeId(memberId, appNotice.getId())
                    .map(this::toReadResponse)
                    .orElseThrow(() -> e);
        }
    }

    private AppNotice findPublishedNoticeOrThrow(String appNoticeId) {
        return appNoticeRepository.findPublishedById(appNoticeId, LocalDateTime.now())
                .orElseThrow(AppNoticeNotFoundException::new);
    }

    private List<String> normalizeImageUrls(List<String> imageUrls) {
        if (imageUrls == null) {
            return List.of();
        }
        return imageUrls.stream()
                .map(this::trimToNull)
                .filter(value -> value != null)
                .toList();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
