package com.skuri.skuri_backend.domain.member.service;

import com.skuri.skuri_backend.domain.chat.websocket.ChatWebSocketSessionRegistry;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.skuri.skuri_backend.domain.member.event.MemberLifecycleEvent;
import com.skuri.skuri_backend.domain.notification.service.NotificationSseService;
import com.skuri.skuri_backend.domain.taxiparty.service.JoinRequestSseService;
import com.skuri.skuri_backend.domain.taxiparty.service.PartySseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberLifecycleEventListener {

    private final ObjectProvider<FirebaseAuth> firebaseAuthProvider;
    private final ChatWebSocketSessionRegistry chatWebSocketSessionRegistry;
    private final NotificationSseService notificationSseService;
    private final PartySseService partySseService;
    private final JoinRequestSseService joinRequestSseService;

    @EventListener
    public void onMemberWithdrawn(MemberLifecycleEvent.MemberWithdrawn event) {
        closeLiveConnections(event.memberId());
        deleteFirebaseUser(event.memberId());
    }

    private void closeLiveConnections(String memberId) {
        chatWebSocketSessionRegistry.revokeMember(memberId);
        chatWebSocketSessionRegistry.closeSessionsForMember(memberId);
        notificationSseService.closeSubscriptionsForMember(memberId);
        partySseService.closeSubscriptionsForMember(memberId);
        joinRequestSseService.closeSubscriptionsForMember(memberId);
    }

    private void deleteFirebaseUser(String memberId) {
        FirebaseAuth firebaseAuth = firebaseAuthProvider.getIfAvailable();
        if (firebaseAuth == null) {
            log.info("FirebaseAuth 빈이 없어 탈퇴 회원 Firebase 삭제를 건너뜁니다. memberId={}", memberId);
            return;
        }

        try {
            firebaseAuth.deleteUser(memberId);
        } catch (FirebaseAuthException e) {
            log.warn("탈퇴 회원 Firebase 삭제 실패: memberId={}, errorCode={}, message={}", memberId, e.getErrorCode(), e.getMessage(), e);
        } catch (Exception e) {
            log.warn("탈퇴 회원 Firebase 삭제 중 예외 발생: memberId={}, message={}", memberId, e.getMessage(), e);
        }
    }
}
