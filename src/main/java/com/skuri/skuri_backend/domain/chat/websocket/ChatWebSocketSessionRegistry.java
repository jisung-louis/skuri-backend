package com.skuri.skuri_backend.domain.chat.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ChatWebSocketSessionRegistry {

    private final ConcurrentHashMap<String, String> sessionToMember = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> memberToSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, WebSocketSession> liveSessions = new ConcurrentHashMap<>();
    private final Set<String> revokedMembers = ConcurrentHashMap.newKeySet();

    public void registerAuthenticatedSession(String memberId, String sessionId) {
        if (memberId == null || sessionId == null) {
            return;
        }

        sessionToMember.put(sessionId, memberId);
        memberToSessions.computeIfAbsent(memberId, ignored -> ConcurrentHashMap.newKeySet()).add(sessionId);
    }

    public void registerTransportSession(WebSocketSession session) {
        if (session == null) {
            return;
        }
        liveSessions.put(session.getId(), session);
    }

    public void unregisterSession(String sessionId) {
        if (sessionId == null) {
            return;
        }

        liveSessions.remove(sessionId);
        String memberId = sessionToMember.remove(sessionId);
        if (memberId == null) {
            return;
        }

        memberToSessions.computeIfPresent(memberId, (ignored, sessionIds) -> {
            sessionIds.remove(sessionId);
            return sessionIds.isEmpty() ? null : sessionIds;
        });
    }

    public Optional<String> findMemberId(String sessionId) {
        return Optional.ofNullable(sessionToMember.get(sessionId));
    }

    public boolean isRevokedSession(String sessionId) {
        return findMemberId(sessionId)
                .map(revokedMembers::contains)
                .orElse(false);
    }

    public void revokeMember(String memberId) {
        if (memberId != null) {
            revokedMembers.add(memberId);
        }
    }

    public void closeSessionsForMember(String memberId) {
        if (memberId == null) {
            return;
        }

        Set<String> sessionIds = memberToSessions.get(memberId);
        if (sessionIds == null || sessionIds.isEmpty()) {
            return;
        }

        for (String sessionId : Set.copyOf(sessionIds)) {
            WebSocketSession session = liveSessions.get(sessionId);
            if (session == null || !session.isOpen()) {
                unregisterSession(sessionId);
                continue;
            }

            try {
                session.close(CloseStatus.POLICY_VIOLATION.withReason("withdrawn-member"));
            } catch (IOException e) {
                log.warn("탈퇴 회원 WebSocket 세션 종료 실패: memberId={}, sessionId={}, message={}", memberId, sessionId, e.getMessage(), e);
            } finally {
                unregisterSession(sessionId);
            }
        }
    }
}
