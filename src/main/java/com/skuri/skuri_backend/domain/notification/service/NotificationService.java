package com.skuri.skuri_backend.domain.notification.service;

import com.skuri.skuri_backend.domain.notification.dto.response.NotificationListResponse;
import com.skuri.skuri_backend.domain.notification.dto.response.NotificationReadAllResponse;
import com.skuri.skuri_backend.domain.notification.dto.response.NotificationResponse;
import com.skuri.skuri_backend.domain.notification.dto.response.NotificationUnreadCountResponse;
import com.skuri.skuri_backend.domain.notification.entity.NotificationType;
import com.skuri.skuri_backend.domain.notification.entity.UserNotification;
import com.skuri.skuri_backend.domain.notification.exception.NotNotificationOwnerException;
import com.skuri.skuri_backend.domain.notification.exception.NotificationNotFoundException;
import com.skuri.skuri_backend.domain.notification.repository.UserNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final List<NotificationType> PARTY_RELATED_TYPES = List.of(
            NotificationType.PARTY_CREATED,
            NotificationType.PARTY_JOIN_REQUEST,
            NotificationType.PARTY_JOIN_ACCEPTED,
            NotificationType.PARTY_JOIN_DECLINED,
            NotificationType.PARTY_CLOSED,
            NotificationType.PARTY_ARRIVED,
            NotificationType.PARTY_ENDED,
            NotificationType.MEMBER_KICKED,
            NotificationType.SETTLEMENT_COMPLETED
    );

    private final UserNotificationRepository userNotificationRepository;
    private final NotificationSseService notificationSseService;

    @Transactional(readOnly = true)
    public NotificationListResponse getNotifications(String memberId, Boolean unreadOnly, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(resolvePage(page), resolveSize(size));
        Page<UserNotification> notifications = Boolean.TRUE.equals(unreadOnly)
                ? userNotificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(memberId, pageable)
                : userNotificationRepository.findByUserIdOrderByCreatedAtDesc(memberId, pageable);

        return new NotificationListResponse(
                notifications.getContent().stream().map(this::toResponse).toList(),
                notifications.getNumber(),
                notifications.getSize(),
                notifications.getTotalElements(),
                notifications.getTotalPages(),
                notifications.hasNext(),
                notifications.hasPrevious(),
                userNotificationRepository.countByUserIdAndReadFalse(memberId)
        );
    }

    @Transactional(readOnly = true)
    public NotificationUnreadCountResponse getUnreadCount(String memberId) {
        return new NotificationUnreadCountResponse(userNotificationRepository.countByUserIdAndReadFalse(memberId));
    }

    @Transactional
    public NotificationResponse markRead(String memberId, String notificationId) {
        UserNotification notification = getOwnedNotification(memberId, notificationId);
        boolean unreadChanged = notification.markRead(LocalDateTime.now());
        if (unreadChanged) {
            publishUnreadCountChangedAfterCommit(memberId, resolveUnreadCountAfterRemoval(memberId, 1));
        }
        return toResponse(notification);
    }

    @Transactional
    public NotificationReadAllResponse markAllRead(String memberId) {
        List<UserNotification> unreadNotifications = userNotificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(memberId);
        if (unreadNotifications.isEmpty()) {
            return new NotificationReadAllResponse(0, 0);
        }

        LocalDateTime now = LocalDateTime.now();
        unreadNotifications.forEach(notification -> notification.markRead(now));
        publishUnreadCountChangedAfterCommit(memberId, 0);

        return new NotificationReadAllResponse(unreadNotifications.size(), 0);
    }

    @Transactional
    public void delete(String memberId, String notificationId) {
        UserNotification notification = getOwnedNotification(memberId, notificationId);
        boolean unreadRemoved = !notification.isRead();
        long unreadCountAfterDelete = unreadRemoved ? resolveUnreadCountAfterRemoval(memberId, 1) : 0;
        userNotificationRepository.delete(notification);
        if (unreadRemoved) {
            publishUnreadCountChangedAfterCommit(memberId, unreadCountAfterDelete);
        }
    }

    @Transactional
    public void createInboxNotifications(NotificationDispatchRequest request) {
        if (!request.inboxEnabled() || request.recipientIds().isEmpty()) {
            return;
        }

        Map<String, Long> unreadCounts = resolveUnreadCounts(request.recipientIds());
        request.recipientIds().forEach(recipientId -> unreadCounts.merge(recipientId, 1L, Long::sum));

        List<UserNotification> saved = userNotificationRepository.saveAll(
                request.recipientIds().stream()
                        .map(recipientId -> UserNotification.create(
                                recipientId,
                                request.type(),
                                request.title(),
                                request.message(),
                                request.data()
                        ))
                        .toList()
        );
        publishCreatedNotificationsAfterCommit(saved, unreadCounts);
    }

    @Transactional
    public void deletePartyRelatedNotifications(String memberId, String partyId) {
        List<UserNotification> notifications = userNotificationRepository.findByUserIdAndTypeInOrderByCreatedAtDesc(
                memberId,
                PARTY_RELATED_TYPES
        );
        List<UserNotification> targets = notifications.stream()
                .filter(notification -> notification.matchesParty(partyId))
                .toList();

        if (targets.isEmpty()) {
            return;
        }

        long unreadRemovedCount = targets.stream().filter(notification -> !notification.isRead()).count();
        long unreadCountAfterDelete = unreadRemovedCount > 0
                ? resolveUnreadCountAfterRemoval(memberId, unreadRemovedCount)
                : 0;
        userNotificationRepository.deleteAllInBatch(targets);
        if (unreadRemovedCount > 0) {
            publishUnreadCountChangedAfterCommit(memberId, unreadCountAfterDelete);
        }
    }

    private UserNotification getOwnedNotification(String memberId, String notificationId) {
        UserNotification notification = userNotificationRepository.findById(notificationId)
                .orElseThrow(NotificationNotFoundException::new);

        if (!notification.belongsTo(memberId)) {
            throw new NotNotificationOwnerException();
        }
        return notification;
    }

    private NotificationResponse toResponse(UserNotification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getData(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }

    private void publishCreatedNotificationsAfterCommit(List<UserNotification> notifications, Map<String, Long> unreadCounts) {
        if (notifications == null || notifications.isEmpty()) {
            return;
        }

        runAfterCommit(() -> {
            notifications.forEach(notification -> notificationSseService.publishNotification(
                    notification.getUserId(),
                    toResponse(notification)
            ));
            unreadCounts.forEach(notificationSseService::publishUnreadCountChanged);
        });
    }

    private void publishUnreadCountChangedAfterCommit(String memberId, long unreadCount) {
        runAfterCommit(() -> notificationSseService.publishUnreadCountChanged(memberId, unreadCount));
    }

    private void runAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
            return;
        }

        task.run();
    }

    private int resolvePage(Integer page) {
        if (page == null || page < 0) {
            return DEFAULT_PAGE;
        }
        return page;
    }

    private int resolveSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private long resolveUnreadCountAfterRemoval(String memberId, long removedCount) {
        long currentUnreadCount = userNotificationRepository.countByUserIdAndReadFalse(memberId);
        return Math.max(currentUnreadCount - removedCount, 0);
    }

    private Map<String, Long> resolveUnreadCounts(Collection<String> memberIds) {
        Map<String, Long> unreadCounts = memberIds.stream()
                .collect(Collectors.toMap(Function.identity(), ignored -> 0L));

        userNotificationRepository.countUnreadByUserIds(memberIds).forEach(count ->
                unreadCounts.put(count.getUserId(), count.getUnreadCount())
        );
        return unreadCounts;
    }
}
