package com.skuri.skuri_backend.domain.taxiparty.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.taxiparty.entity.JoinRequest;
import com.skuri.skuri_backend.domain.taxiparty.entity.JoinRequestStatus;
import com.skuri.skuri_backend.domain.taxiparty.entity.Location;
import com.skuri.skuri_backend.domain.taxiparty.entity.Party;
import com.skuri.skuri_backend.domain.taxiparty.repository.JoinRequestRepository;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JoinRequestSseSnapshotServiceTest {

    @Mock
    private PartyRepository partyRepository;

    @Mock
    private JoinRequestRepository joinRequestRepository;

    @Mock
    private MemberRepository memberRepository;

    @Test
    @SuppressWarnings("unchecked")
    void createPartyJoinRequestsSnapshotPayload_리더검증후_요청목록을계산한다() {
        JoinRequestSseSnapshotService snapshotService =
                new JoinRequestSseSnapshotService(partyRepository, joinRequestRepository, memberRepository);
        Party party = sampleParty("party-1", "leader-1");
        JoinRequest request = sampleJoinRequest("request-1", party, "requester-1");
        Member requester = Member.create("requester-1", "requester@sungkyul.ac.kr", "요청자", LocalDateTime.now());

        when(partyRepository.findById("party-1")).thenReturn(Optional.of(party));
        when(joinRequestRepository.findByParty_IdOrderByCreatedAtDesc("party-1")).thenReturn(List.of(request));
        when(memberRepository.findAllById(List.of("requester-1"))).thenReturn(List.of(requester));

        Map<String, Object> payload = snapshotService.createPartyJoinRequestsSnapshotPayload("leader-1", "party-1");

        assertEquals("party-1", payload.get("partyId"));
        assertEquals(1, ((List<?>) payload.get("requests")).size());
        verify(partyRepository).findById("party-1");
        verify(joinRequestRepository).findByParty_IdOrderByCreatedAtDesc("party-1");
        verify(memberRepository).findAllById(List.of("requester-1"));
    }

    @Test
    void createPartyJoinRequestsSnapshotPayload_리더아니면_NOT_PARTY_LEADER() {
        JoinRequestSseSnapshotService snapshotService =
                new JoinRequestSseSnapshotService(partyRepository, joinRequestRepository, memberRepository);
        Party party = sampleParty("party-1", "leader-1");
        when(partyRepository.findById("party-1")).thenReturn(Optional.of(party));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> snapshotService.createPartyJoinRequestsSnapshotPayload("member-2", "party-1")
        );

        assertEquals(ErrorCode.NOT_PARTY_LEADER, exception.getErrorCode());
    }

    @Test
    void createMyJoinRequestsSnapshotPayload_status필터에맞게_요청목록을계산한다() {
        JoinRequestSseSnapshotService snapshotService =
                new JoinRequestSseSnapshotService(partyRepository, joinRequestRepository, memberRepository);
        Party party = sampleParty("party-1", "leader-1");
        JoinRequest request = sampleJoinRequest("request-1", party, "requester-1");
        request.accept();
        Member requester = Member.create("requester-1", "requester@sungkyul.ac.kr", "요청자", LocalDateTime.now());

        when(joinRequestRepository.findByRequesterIdAndStatusOrderByCreatedAtDesc("requester-1", JoinRequestStatus.ACCEPTED))
                .thenReturn(List.of(request));
        when(memberRepository.findAllById(List.of("requester-1"))).thenReturn(List.of(requester));

        Map<String, Object> payload =
                snapshotService.createMyJoinRequestsSnapshotPayload("requester-1", JoinRequestStatus.ACCEPTED);

        assertEquals(1, ((List<?>) payload.get("requests")).size());
        verify(joinRequestRepository).findByRequesterIdAndStatusOrderByCreatedAtDesc("requester-1", JoinRequestStatus.ACCEPTED);
        verify(memberRepository).findAllById(List.of("requester-1"));
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

    private JoinRequest sampleJoinRequest(String requestId, Party party, String requesterId) {
        JoinRequest request = JoinRequest.create(party, requesterId);
        ReflectionTestUtils.setField(request, "id", requestId);
        ReflectionTestUtils.setField(request, "createdAt", LocalDateTime.now());
        return request;
    }
}
