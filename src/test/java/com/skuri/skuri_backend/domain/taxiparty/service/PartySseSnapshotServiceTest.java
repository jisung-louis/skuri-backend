package com.skuri.skuri_backend.domain.taxiparty.service;

import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartySummaryResponse;
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
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.argThat;
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
        Party party = sampleParty("party-1", "leader-1", "member-1");
        Member leader = Member.create("leader-1", "leader@sungkyul.ac.kr", "리더", LocalDateTime.now());
        leader.updateProfile("리더", null, null, "https://cdn.skuri.app/uploads/profiles/leader.jpg");
        Member member = Member.create("member-1", "member-1@sungkyul.ac.kr", "멤버", LocalDateTime.now());
        member.updateProfile("김민수", null, null, null);

        when(partyRepository.findSseSnapshotParties()).thenReturn(List.of(party));
        when(memberRepository.findAllById(argThat(this::matchesMemberIds)))
                .thenReturn(List.of(leader, member));

        Map<String, Object> payload = snapshotService.createSnapshotPayload();

        assertNotNull(payload);
        List<PartySummaryResponse> parties = (List<PartySummaryResponse>) payload.get("parties");
        assertEquals(1, parties.size());
        assertEquals(2, parties.getFirst().participantSummaries().size());
        assertEquals("leader-1", parties.getFirst().participantSummaries().get(0).id());
        assertEquals("https://cdn.skuri.app/uploads/profiles/leader.jpg", parties.getFirst().participantSummaries().get(0).photoUrl());
        assertEquals("member-1", parties.getFirst().participantSummaries().get(1).id());
        verify(partyRepository).findSseSnapshotParties();
        verify(memberRepository).findAllById(argThat(this::matchesMemberIds));
    }

    private boolean matchesMemberIds(Iterable<String> ids) {
        List<String> actualIds = StreamSupport.stream(ids.spliterator(), false).toList();
        return actualIds.containsAll(List.of("leader-1", "member-1")) && actualIds.size() == 2;
    }

    private Party sampleParty(String partyId, String leaderId, String... memberIds) {
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
        for (String memberId : memberIds) {
            party.addMember(memberId);
        }
        return party;
    }
}
