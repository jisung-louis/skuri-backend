package com.skuri.skuri_backend.domain.taxiparty.service;

import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyLocationResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartySummaryResponse;
import com.skuri.skuri_backend.domain.taxiparty.entity.Party;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartySseService {

    private static final long SSE_TIMEOUT_MILLIS = 60L * 60L * 1000L;
    private static final long SSE_RETRY_MILLIS = 3_000L;

    private final PartyRepository partyRepository;
    private final MemberRepository memberRepository;

    private final Map<String, SseSubscriber> subscribers = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public SseEmitter subscribeParties(String memberId) {
        String emitterId = memberId + ":" + UUID.randomUUID();
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        subscribers.put(emitterId, new SseSubscriber(memberId, emitter));

        emitter.onCompletion(() -> subscribers.remove(emitterId));
        emitter.onTimeout(() -> {
            subscribers.remove(emitterId);
            safeComplete(emitter);
        });
        emitter.onError(ex -> {
            subscribers.remove(emitterId);
            log.debug("파티 SSE 연결 오류로 구독 해제: emitterId={}", emitterId, ex);
        });

        sendToOne(
                emitterId,
                "SNAPSHOT",
                Map.of("parties", getSnapshotParties())
        );
        return emitter;
    }

    public void publishHeartbeat() {
        if (subscribers.isEmpty()) {
            return;
        }
        broadcast("HEARTBEAT", Map.of("timestamp", LocalDateTime.now()));
    }

    public void closeSubscriptionsForMember(String memberId) {
        subscribers.forEach((emitterId, subscriber) -> {
            if (!memberId.equals(subscriber.memberId())) {
                return;
            }
            subscribers.remove(emitterId);
            try {
                subscriber.emitter().complete();
            } catch (Exception ignored) {
                // no-op
            }
        });
    }

    public void publishPartyCreated(Party party, Member leader) {
        publishAfterCommit("PARTY_CREATED", toPartySummaryResponse(party, leader));
    }

    public void publishPartyUpdated(Party party, Member leader) {
        publishAfterCommit("PARTY_UPDATED", toPartySummaryResponse(party, leader));
    }

    public void publishPartyStatusChanged(Party party) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", party.getId());
        payload.put("status", party.getStatus());
        payload.put("currentMembers", party.getCurrentMembers());
        if (party.getEndReason() != null) {
            payload.put("endReason", party.getEndReason());
        }
        publishAfterCommit("PARTY_STATUS_CHANGED", payload);
    }

    public void publishPartyDeleted(String partyId) {
        publishAfterCommit("PARTY_DELETED", Map.of("id", partyId));
    }

    public void publishPartyMemberJoined(
            Party party,
            String memberId,
            String memberName,
            Collection<String> recipientMemberIds
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("partyId", party.getId());
        payload.put("memberId", memberId);
        payload.put("memberName", memberName);
        payload.put("currentMembers", party.getCurrentMembers());
        publishAfterCommitToMembers("PARTY_MEMBER_JOINED", payload, recipientMemberIds);
    }

    public void publishPartyMemberLeft(
            Party party,
            String memberId,
            String reason,
            Collection<String> recipientMemberIds
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("partyId", party.getId());
        payload.put("memberId", memberId);
        payload.put("reason", reason);
        payload.put("currentMembers", party.getCurrentMembers());
        publishAfterCommitToMembers("PARTY_MEMBER_LEFT", payload, recipientMemberIds);
    }

    private void publishAfterCommit(String eventType, Object payload) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    broadcast(eventType, payload);
                }
            });
            return;
        }
        broadcast(eventType, payload);
    }

    private void publishAfterCommitToMembers(String eventType, Object payload, Collection<String> memberIds) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    broadcastToMembers(eventType, payload, memberIds);
                }
            });
            return;
        }
        broadcastToMembers(eventType, payload, memberIds);
    }

    private void broadcast(String eventType, Object payload) {
        if (subscribers.isEmpty()) {
            return;
        }
        subscribers.forEach((emitterId, subscriber) -> sendToOne(emitterId, eventType, payload));
    }

    private void broadcastToMembers(String eventType, Object payload, Collection<String> memberIds) {
        if (subscribers.isEmpty() || memberIds == null || memberIds.isEmpty()) {
            return;
        }
        subscribers.forEach((emitterId, subscriber) -> {
            if (memberIds.contains(subscriber.memberId())) {
                sendToOne(emitterId, eventType, payload);
            }
        });
    }

    private void sendToOne(String emitterId, String eventType, Object payload) {
        SseSubscriber subscriber = subscribers.get(emitterId);
        if (subscriber == null) {
            return;
        }

        try {
            subscriber.emitter().send(
                    SseEmitter.event()
                            .id(String.valueOf(System.currentTimeMillis()))
                            .name(eventType)
                            .reconnectTime(SSE_RETRY_MILLIS)
                            .data(payload)
            );
        } catch (IOException | IllegalStateException e) {
            subscribers.remove(emitterId);
            log.debug("SSE 전송 실패로 구독 해제: emitterId={}, eventType={}", emitterId, eventType);
        }
    }

    private void safeComplete(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (Exception ignored) {
            // no-op
        }
    }

    private List<PartySummaryResponse> getSnapshotParties() {
        List<Party> parties = partyRepository.findSseSnapshotParties();
        List<String> leaderIds = parties.stream().map(Party::getLeaderId).distinct().toList();
        Map<String, Member> leaderMap = new HashMap<>();
        memberRepository.findAllById(leaderIds).forEach(member -> leaderMap.put(member.getId(), member));

        return parties.stream()
                .map(party -> toPartySummaryResponse(party, leaderMap.get(party.getLeaderId())))
                .toList();
    }

    private PartySummaryResponse toPartySummaryResponse(Party party, Member leader) {
        return new PartySummaryResponse(
                party.getId(),
                party.getLeaderId(),
                leader != null ? leader.getNickname() : null,
                leader != null ? leader.getPhotoUrl() : null,
                new PartyLocationResponse(party.getDeparture().getName(), party.getDeparture().getLat(), party.getDeparture().getLng()),
                new PartyLocationResponse(party.getDestination().getName(), party.getDestination().getLat(), party.getDestination().getLng()),
                party.getDepartureTime(),
                party.getMaxMembers(),
                party.getCurrentMembers(),
                party.getTagsText(),
                party.getDetail(),
                party.getStatus(),
                party.getCreatedAt()
        );
    }

    private record SseSubscriber(
            String memberId,
            SseEmitter emitter
    ) {
    }
}
