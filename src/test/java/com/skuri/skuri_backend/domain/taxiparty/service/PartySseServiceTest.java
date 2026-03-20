package com.skuri.skuri_backend.domain.taxiparty.service;

import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.taxiparty.entity.Location;
import com.skuri.skuri_backend.domain.taxiparty.entity.Party;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartySseServiceTest {

    @Mock
    private PartyRepository partyRepository;

    @Mock
    private MemberRepository memberRepository;

    @Test
    void subscribeParties_호출시_스냅샷조회수행() {
        PartySseService partySseService = new PartySseService(partyRepository, memberRepository);
        Party party = sampleParty("party-1", "leader-1");
        Member leader = Member.create("leader-1", "leader@sungkyul.ac.kr", "리더", LocalDateTime.now());

        when(partyRepository.findSseSnapshotParties()).thenReturn(List.of(party));
        when(memberRepository.findAllById(List.of("leader-1"))).thenReturn(List.of(leader));

        SseEmitter emitter = partySseService.subscribeParties("member-1");

        assertNotNull(emitter);
        verify(partyRepository).findSseSnapshotParties();
        verify(memberRepository).findAllById(List.of("leader-1"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void publishHeartbeat_전송실패시_구독해제() throws Exception {
        PartySseService partySseService = new PartySseService(partyRepository, memberRepository);
        Map<String, Object> subscribers = (Map<String, Object>) ReflectionTestUtils.getField(partySseService, "subscribers");
        AtomicInteger completeCount = new AtomicInteger();

        SseEmitter failingEmitter = new SseEmitter() {
            @Override
            public synchronized void send(SseEventBuilder builder) throws IOException {
                throw new IOException("send failed");
            }

            @Override
            public synchronized void complete() {
                completeCount.incrementAndGet();
            }
        };

        subscribers.put("member-1:emitter-1", createSubscriber("member-1", failingEmitter));

        partySseService.publishHeartbeat();

        assertTrue(subscribers.isEmpty());
        assertEquals(0, completeCount.get());
    }

    private Object createSubscriber(String memberId, SseEmitter emitter) throws Exception {
        Class<?> subscriberClass = Class.forName("com.skuri.skuri_backend.domain.taxiparty.service.PartySseService$SseSubscriber");
        Constructor<?> constructor = subscriberClass.getDeclaredConstructor(String.class, SseEmitter.class);
        constructor.setAccessible(true);
        return constructor.newInstance(memberId, emitter);
    }

    private Party sampleParty(String partyId, String leaderId) {
        Party party = Party.create(
                leaderId,
                Location.of("성결대학교", 37.38, 126.93),
                Location.of("안양역", 37.40, 126.92),
                LocalDateTime.now().plusHours(1),
                4,
                List.of("빠른출발"),
                "테스트"
        );
        ReflectionTestUtils.setField(party, "id", partyId);
        return party;
    }
}
