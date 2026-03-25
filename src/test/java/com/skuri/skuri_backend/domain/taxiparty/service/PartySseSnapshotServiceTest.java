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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartySseSnapshotServiceTest {

    @Mock
    private PartyRepository partyRepository;

    @Mock
    private MemberRepository memberRepository;

    @Test
    @SuppressWarnings("unchecked")
    void createSnapshotPayload_파티와리더정보를DTO로계산한다() {
        PartySseSnapshotService snapshotService = new PartySseSnapshotService(partyRepository, memberRepository);
        Party party = sampleParty("party-1", "leader-1");
        Member leader = Member.create("leader-1", "leader@sungkyul.ac.kr", "리더", LocalDateTime.now());

        when(partyRepository.findSseSnapshotParties()).thenReturn(List.of(party));
        when(memberRepository.findAllById(List.of("leader-1"))).thenReturn(List.of(leader));

        Map<String, Object> payload = snapshotService.createSnapshotPayload();

        assertNotNull(payload);
        List<?> parties = (List<?>) payload.get("parties");
        assertEquals(1, parties.size());
        verify(partyRepository).findSseSnapshotParties();
        verify(memberRepository).findAllById(List.of("leader-1"));
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
