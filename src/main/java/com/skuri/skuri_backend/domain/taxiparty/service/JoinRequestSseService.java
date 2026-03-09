package com.skuri.skuri_backend.domain.taxiparty.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.JoinRequestListItemResponse;
import com.skuri.skuri_backend.domain.taxiparty.entity.JoinRequest;
import com.skuri.skuri_backend.domain.taxiparty.entity.JoinRequestStatus;
import com.skuri.skuri_backend.domain.taxiparty.entity.Party;
import com.skuri.skuri_backend.domain.taxiparty.exception.PartyNotFoundException;
import com.skuri.skuri_backend.domain.taxiparty.repository.JoinRequestRepository;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class JoinRequestSseService {

    private static final long SSE_TIMEOUT_MILLIS = 60L * 60L * 1000L;
    private static final long SSE_RETRY_MILLIS = 3_000L;
    private static final int SUBSCRIBER_WARNING_THRESHOLD = 500;
    private static final int FAILURE_RATE_WINDOW_MINUTES = 5;
    private static final double FAILURE_RATE_WARNING_THRESHOLD = 5.0;

    private final PartyRepository partyRepository;
    private final JoinRequestRepository joinRequestRepository;
    private final MemberRepository memberRepository;

    private final Map<String, PartyJoinRequestSubscriber> partyJoinRequestSubscribers = new ConcurrentHashMap<>();
    private final Map<String, MyJoinRequestSubscriber> myJoinRequestSubscribers = new ConcurrentHashMap<>();

    private final AtomicLong windowSendCount = new AtomicLong();
    private final AtomicLong windowFailureCount = new AtomicLong();
    private volatile LocalDateTime failureRateWindowStartedAt = LocalDateTime.now();

    @Transactional(readOnly = true)
    public SseEmitter subscribePartyJoinRequests(String actorId, String partyId) {
        Party party = partyRepository.findById(partyId).orElseThrow(PartyNotFoundException::new);
        if (!party.isLeader(actorId)) {
            throw new BusinessException(ErrorCode.NOT_PARTY_LEADER);
        }

        String emitterId = "party:" + partyId + ":leader:" + actorId + ":" + UUID.randomUUID();
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        partyJoinRequestSubscribers.put(emitterId, new PartyJoinRequestSubscriber(partyId, actorId, emitter));
        registerPartySubscriberLifecycle(emitterId, emitter);
        logSubscriberSize("subscribe", emitterId);

        sendToPartySubscriber(
                emitterId,
                "SNAPSHOT",
                Map.of(
                        "partyId", partyId,
                        "requests", getPartyJoinRequestSnapshot(partyId)
                )
        );
        return emitter;
    }

    @Transactional(readOnly = true)
    public SseEmitter subscribeMyJoinRequests(String memberId, JoinRequestStatus status) {
        String statusOrAll = status == null ? "ALL" : status.name();
        String emitterId = "my-requests:" + memberId + ":" + statusOrAll + ":" + UUID.randomUUID();
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        myJoinRequestSubscribers.put(emitterId, new MyJoinRequestSubscriber(memberId, status, emitter));
        registerMySubscriberLifecycle(emitterId, emitter);
        logSubscriberSize("subscribe", emitterId);

        sendToMySubscriber(
                emitterId,
                "SNAPSHOT",
                Map.of("requests", getMyJoinRequestSnapshot(memberId, status))
        );
        return emitter;
    }

    public void publishJoinRequestCreated(JoinRequest joinRequest) {
        JoinRequestListItemResponse payload = toSseItem(joinRequest);
        publishAfterCommit(() -> {
            int leaderRecipients = broadcastToPartyLeaders(
                    joinRequest.getParty().getId(),
                    joinRequest.getLeaderId(),
                    "JOIN_REQUEST_CREATED",
                    payload
            );
            int requesterRecipients = broadcastToMyJoinRequests(
                    joinRequest.getRequesterId(),
                    null,
                    joinRequest.getStatus(),
                    "MY_JOIN_REQUEST_CREATED",
                    payload
            );
            logEventPublish("JOIN_REQUEST_CREATED", leaderRecipients);
            logEventPublish("MY_JOIN_REQUEST_CREATED", requesterRecipients);
        });
    }

    public void publishJoinRequestUpdated(JoinRequest joinRequest, JoinRequestStatus previousStatus) {
        JoinRequestListItemResponse payload = toSseItem(joinRequest);
        publishAfterCommit(() -> {
            int leaderRecipients = broadcastToPartyLeaders(
                    joinRequest.getParty().getId(),
                    joinRequest.getLeaderId(),
                    "JOIN_REQUEST_UPDATED",
                    payload
            );
            int requesterRecipients = broadcastToMyJoinRequests(
                    joinRequest.getRequesterId(),
                    previousStatus,
                    joinRequest.getStatus(),
                    "MY_JOIN_REQUEST_UPDATED",
                    payload
            );
            logEventPublish("JOIN_REQUEST_UPDATED", leaderRecipients);
            logEventPublish("MY_JOIN_REQUEST_UPDATED", requesterRecipients);
        });
    }

    public void publishHeartbeat() {
        if (partyJoinRequestSubscribers.isEmpty() && myJoinRequestSubscribers.isEmpty()) {
            return;
        }

        Map<String, Object> payload = Map.of("timestamp", LocalDateTime.now());

        int partyRecipients = 0;
        for (String emitterId : partyJoinRequestSubscribers.keySet()) {
            if (sendToPartySubscriber(emitterId, "HEARTBEAT", payload)) {
                partyRecipients++;
            }
        }
        int myRecipients = 0;
        for (String emitterId : myJoinRequestSubscribers.keySet()) {
            if (sendToMySubscriber(emitterId, "HEARTBEAT", payload)) {
                myRecipients++;
            }
        }
        logEventPublish("HEARTBEAT", partyRecipients + myRecipients);
    }

    public void closeSubscriptionsForMember(String memberId) {
        partyJoinRequestSubscribers.forEach((emitterId, subscriber) -> {
            if (!memberId.equals(subscriber.leaderId())) {
                return;
            }
            partyJoinRequestSubscribers.remove(emitterId);
            safeComplete(subscriber.emitter());
        });

        myJoinRequestSubscribers.forEach((emitterId, subscriber) -> {
            if (!memberId.equals(subscriber.memberId())) {
                return;
            }
            myJoinRequestSubscribers.remove(emitterId);
            safeComplete(subscriber.emitter());
        });
    }

    private void publishAfterCommit(Runnable publisher) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publisher.run();
                }
            });
            return;
        }
        publisher.run();
    }

    private List<JoinRequestListItemResponse> getPartyJoinRequestSnapshot(String partyId) {
        List<JoinRequest> requests = joinRequestRepository.findByParty_IdOrderByCreatedAtDesc(partyId);
        return toSseItems(requests);
    }

    private List<JoinRequestListItemResponse> getMyJoinRequestSnapshot(String memberId, JoinRequestStatus status) {
        List<JoinRequest> requests = status == null
                ? joinRequestRepository.findByRequesterIdOrderByCreatedAtDesc(memberId)
                : joinRequestRepository.findByRequesterIdAndStatusOrderByCreatedAtDesc(memberId, status);
        return toSseItems(requests);
    }

    private List<JoinRequestListItemResponse> toSseItems(List<JoinRequest> requests) {
        Map<String, Member> requesterMap = getMemberMap(requests.stream().map(JoinRequest::getRequesterId).toList());
        return requests.stream()
                .map(request -> toSseItem(request, requesterMap.get(request.getRequesterId())))
                .toList();
    }

    private JoinRequestListItemResponse toSseItem(JoinRequest joinRequest) {
        Member requester = memberRepository.findById(joinRequest.getRequesterId()).orElse(null);
        return toSseItem(joinRequest, requester);
    }

    private JoinRequestListItemResponse toSseItem(JoinRequest joinRequest, Member requester) {
        return new JoinRequestListItemResponse(
                joinRequest.getId(),
                joinRequest.getParty().getId(),
                joinRequest.getRequesterId(),
                requester != null ? requester.getNickname() : null,
                requester != null ? requester.getPhotoUrl() : null,
                joinRequest.getStatus(),
                joinRequest.getCreatedAt()
        );
    }

    private Map<String, Member> getMemberMap(List<String> memberIds) {
        List<String> ids = memberIds.stream().distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<String, Member> memberMap = new HashMap<>();
        memberRepository.findAllById(ids).forEach(member -> memberMap.put(member.getId(), member));
        return memberMap;
    }

    private int broadcastToPartyLeaders(String partyId, String leaderId, String eventType, Object payload) {
        if (partyJoinRequestSubscribers.isEmpty()) {
            return 0;
        }

        int recipients = 0;
        for (Map.Entry<String, PartyJoinRequestSubscriber> entry : partyJoinRequestSubscribers.entrySet()) {
            PartyJoinRequestSubscriber subscriber = entry.getValue();
            if (!subscriber.partyId().equals(partyId) || !subscriber.leaderId().equals(leaderId)) {
                continue;
            }
            if (sendToPartySubscriber(entry.getKey(), eventType, payload)) {
                recipients++;
            }
        }
        return recipients;
    }

    private int broadcastToMyJoinRequests(
            String memberId,
            JoinRequestStatus previousStatus,
            JoinRequestStatus currentStatus,
            String eventType,
            Object payload
    ) {
        if (myJoinRequestSubscribers.isEmpty()) {
            return 0;
        }

        int recipients = 0;
        for (Map.Entry<String, MyJoinRequestSubscriber> entry : myJoinRequestSubscribers.entrySet()) {
            MyJoinRequestSubscriber subscriber = entry.getValue();
            if (!subscriber.memberId().equals(memberId)) {
                continue;
            }
            if (!matchesMyStatusFilter(subscriber.statusFilter(), previousStatus, currentStatus)) {
                continue;
            }
            if (sendToMySubscriber(entry.getKey(), eventType, payload)) {
                recipients++;
            }
        }
        return recipients;
    }

    private boolean matchesMyStatusFilter(
            JoinRequestStatus statusFilter,
            JoinRequestStatus previousStatus,
            JoinRequestStatus currentStatus
    ) {
        if (statusFilter == null) {
            return true;
        }
        return statusFilter == currentStatus || (previousStatus != null && statusFilter == previousStatus);
    }

    private boolean sendToPartySubscriber(String emitterId, String eventType, Object payload) {
        PartyJoinRequestSubscriber subscriber = partyJoinRequestSubscribers.get(emitterId);
        if (subscriber == null) {
            return false;
        }

        try {
            subscriber.emitter().send(
                    SseEmitter.event()
                            .id(String.valueOf(System.currentTimeMillis()))
                            .name(eventType)
                            .reconnectTime(SSE_RETRY_MILLIS)
                            .data(payload)
            );
            recordSendResult(true);
            return true;
        } catch (IOException | IllegalStateException e) {
            partyJoinRequestSubscribers.remove(emitterId);
            safeComplete(subscriber.emitter());
            recordSendResult(false);
            log.warn("JoinRequest SSE 전송 실패(파티 구독 해제): emitterId={}, eventType={}", emitterId, eventType);
            logSubscriberSize("cleanup-on-send-fail", emitterId);
            return false;
        }
    }

    private boolean sendToMySubscriber(String emitterId, String eventType, Object payload) {
        MyJoinRequestSubscriber subscriber = myJoinRequestSubscribers.get(emitterId);
        if (subscriber == null) {
            return false;
        }

        try {
            subscriber.emitter().send(
                    SseEmitter.event()
                            .id(String.valueOf(System.currentTimeMillis()))
                            .name(eventType)
                            .reconnectTime(SSE_RETRY_MILLIS)
                            .data(payload)
            );
            recordSendResult(true);
            return true;
        } catch (IOException | IllegalStateException e) {
            myJoinRequestSubscribers.remove(emitterId);
            safeComplete(subscriber.emitter());
            recordSendResult(false);
            log.warn("JoinRequest SSE 전송 실패(내 요청 구독 해제): emitterId={}, eventType={}", emitterId, eventType);
            logSubscriberSize("cleanup-on-send-fail", emitterId);
            return false;
        }
    }

    private void registerPartySubscriberLifecycle(String emitterId, SseEmitter emitter) {
        emitter.onCompletion(() -> {
            partyJoinRequestSubscribers.remove(emitterId);
            logSubscriberSize("complete", emitterId);
        });
        emitter.onTimeout(() -> {
            partyJoinRequestSubscribers.remove(emitterId);
            logSubscriberSize("timeout", emitterId);
            safeComplete(emitter);
        });
        emitter.onError(ex -> {
            partyJoinRequestSubscribers.remove(emitterId);
            logSubscriberSize("error", emitterId);
            safeCompleteWithError(emitter, ex);
        });
    }

    private void registerMySubscriberLifecycle(String emitterId, SseEmitter emitter) {
        emitter.onCompletion(() -> {
            myJoinRequestSubscribers.remove(emitterId);
            logSubscriberSize("complete", emitterId);
        });
        emitter.onTimeout(() -> {
            myJoinRequestSubscribers.remove(emitterId);
            logSubscriberSize("timeout", emitterId);
            safeComplete(emitter);
        });
        emitter.onError(ex -> {
            myJoinRequestSubscribers.remove(emitterId);
            logSubscriberSize("error", emitterId);
            safeCompleteWithError(emitter, ex);
        });
    }

    private void safeComplete(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (Exception ignored) {
            // no-op
        }
    }

    private void safeCompleteWithError(SseEmitter emitter, Throwable throwable) {
        try {
            emitter.completeWithError(throwable);
        } catch (Exception ignored) {
            // no-op
        }
    }

    private void logSubscriberSize(String action, String emitterId) {
        int partySize = partyJoinRequestSubscribers.size();
        int mySize = myJoinRequestSubscribers.size();
        if (partySize > SUBSCRIBER_WARNING_THRESHOLD || mySize > SUBSCRIBER_WARNING_THRESHOLD) {
            log.warn(
                    "JoinRequest SSE subscriber threshold exceeded: action={}, emitterId={}, partySubscribers={}, mySubscribers={}",
                    action,
                    emitterId,
                    partySize,
                    mySize
            );
            return;
        }
        log.debug(
                "JoinRequest SSE subscriber changed: action={}, emitterId={}, partySubscribers={}, mySubscribers={}",
                action,
                emitterId,
                partySize,
                mySize
        );
    }

    private void logEventPublish(String eventType, int recipients) {
        log.info("JoinRequest SSE publish: eventType={}, recipients={}", eventType, recipients);
    }

    private void recordSendResult(boolean success) {
        windowSendCount.incrementAndGet();
        if (!success) {
            windowFailureCount.incrementAndGet();
        }
        evaluateFailureRateWindow();
    }

    private synchronized void evaluateFailureRateWindow() {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(failureRateWindowStartedAt.plusMinutes(FAILURE_RATE_WINDOW_MINUTES))) {
            return;
        }

        long total = windowSendCount.getAndSet(0);
        long failed = windowFailureCount.getAndSet(0);
        failureRateWindowStartedAt = now;

        if (total == 0) {
            return;
        }

        double failureRate = (failed * 100.0) / total;
        if (failureRate > FAILURE_RATE_WARNING_THRESHOLD) {
            log.warn(
                    "JoinRequest SSE send failure rate warning: window={}m, failed={}, total={}, rate={}",
                    FAILURE_RATE_WINDOW_MINUTES,
                    failed,
                    total,
                    String.format("%.2f%%", failureRate)
            );
        }
    }

    private record PartyJoinRequestSubscriber(
            String partyId,
            String leaderId,
            SseEmitter emitter
    ) {
    }

    private record MyJoinRequestSubscriber(
            String memberId,
            JoinRequestStatus statusFilter,
            SseEmitter emitter
    ) {
    }
}
