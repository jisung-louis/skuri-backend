package com.skuri.skuri_backend.domain.taxiparty.service;

import com.skuri.skuri_backend.common.event.AfterCommitApplicationEventPublisher;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomRepository;
import com.skuri.skuri_backend.domain.chat.service.ChatService;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.taxiparty.constant.AdminPartyStatusAction;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyStatusResponse;
import com.skuri.skuri_backend.domain.taxiparty.entity.Location;
import com.skuri.skuri_backend.domain.taxiparty.entity.Party;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyEndReason;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus;
import com.skuri.skuri_backend.domain.taxiparty.entity.SettlementAccountSnapshot;
import com.skuri.skuri_backend.domain.taxiparty.entity.SettlementTargetSnapshot;
import com.skuri.skuri_backend.domain.taxiparty.repository.JoinRequestRepository;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaxiPartyAdminServiceTest {

    @Mock
    private PartyRepository partyRepository;

    @Mock
    private JoinRequestRepository joinRequestRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatService chatService;

    @Mock
    private PartySseService partySseService;

    @Mock
    private AfterCommitApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TaxiPartyAdminService taxiPartyAdminService;

    @Test
    void updatePartyStatus_END_정상처리시_리더기준_END메시지를재사용한다() {
        Party party = sampleParty("party-1", "leader");
        arrive(party);
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(partyRepository.saveAndFlush(any(Party.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PartyStatusResponse response = taxiPartyAdminService.updatePartyStatus("party-1", AdminPartyStatusAction.END);

        assertEquals(PartyStatus.ENDED, response.status());
        assertEquals(PartyEndReason.FORCE_ENDED, response.endReason());
        verify(chatService).createPartyEndMessage(party, "leader");
        verify(partySseService).publishPartyStatusChanged(party);
        verify(eventPublisher).publish(any());
    }

    @Test
    void updatePartyStatus_END_허용되지않는전이면_실패한다() {
        Party party = sampleParty("party-1", "leader");
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> taxiPartyAdminService.updatePartyStatus("party-1", AdminPartyStatusAction.END)
        );

        assertEquals(ErrorCode.INVALID_PARTY_STATE_TRANSITION, exception.getErrorCode());
        verify(partyRepository, never()).saveAndFlush(any(Party.class));
        verify(chatService, never()).createPartyEndMessage(any(), any());
    }

    private Party sampleParty(String partyId, String leaderId) {
        Party party = Party.create(
                leaderId,
                Location.of("성결대학교", 37.38, 126.93),
                Location.of("안양역", 37.40, 126.92),
                LocalDateTime.now().plusHours(1),
                4,
                List.of("빠른출발"),
                "택시비 나눠요"
        );
        ReflectionTestUtils.setField(party, "id", partyId);
        party.addMember("member-1");
        return party;
    }

    private void arrive(Party party) {
        party.arriveWithSnapshots(
                14000,
                List.of(new SettlementTargetSnapshot("member-1", "홍길동")),
                SettlementAccountSnapshot.of("카카오뱅크", "3333-01-1234567", "홍길동", true)
        );
    }
}
