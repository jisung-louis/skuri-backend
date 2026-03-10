package com.skuri.skuri_backend.domain.taxiparty.service;

import com.skuri.skuri_backend.common.event.AfterCommitApplicationEventPublisher;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.chat.service.ChatService;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.taxiparty.dto.request.CreatePartyRequest;
import com.skuri.skuri_backend.domain.taxiparty.dto.request.PartyLocationRequest;
import com.skuri.skuri_backend.domain.taxiparty.dto.request.UpdatePartyRequest;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.JoinRequestAcceptResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.JoinRequestResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyCreateResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyDetailResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyStatusResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.SettlementConfirmResponse;
import com.skuri.skuri_backend.domain.taxiparty.entity.JoinRequest;
import com.skuri.skuri_backend.domain.taxiparty.entity.JoinRequestStatus;
import com.skuri.skuri_backend.domain.taxiparty.entity.Location;
import com.skuri.skuri_backend.domain.taxiparty.entity.Party;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyEndReason;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus;
import com.skuri.skuri_backend.domain.taxiparty.entity.SettlementStatus;
import com.skuri.skuri_backend.domain.taxiparty.repository.JoinRequestRepository;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaxiPartyServiceTest {

    @Mock
    private PartyRepository partyRepository;

    @Mock
    private JoinRequestRepository joinRequestRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PartySseService partySseService;

    @Mock
    private JoinRequestSseService joinRequestSseService;

    @Mock
    private ChatService chatService;

    @Mock
    private AfterCommitApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TaxiPartyService taxiPartyService;

    @Test
    void createParty_정상생성() {
        CreatePartyRequest request = createPartyRequest(4);
        when(memberRepository.findActiveByIdForUpdate("leader")).thenReturn(Optional.of(member("leader")));
        when(partyRepository.existsActivePartyByMemberId(eq("leader"), anySet(), isNull())).thenReturn(false);
        when(partyRepository.save(any(Party.class))).thenAnswer(invocation -> {
            Party saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", "party-created");
            return saved;
        });

        PartyCreateResponse response = taxiPartyService.createParty("leader", request);

        assertEquals("party-created", response.id());
        assertEquals("party:party-created", response.chatRoomId());
        verify(chatService).createPartyChatRoom(any(Party.class));
        verify(partySseService).publishPartyCreated(any(Party.class), eq(null));
    }

    @Test
    void createParty_활성파티가있으면_실패() {
        when(memberRepository.findActiveByIdForUpdate("leader")).thenReturn(Optional.of(member("leader")));
        when(partyRepository.existsActivePartyByMemberId(eq("leader"), anySet(), isNull())).thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> taxiPartyService.createParty("leader", createPartyRequest(4))
        );

        assertEquals(ErrorCode.ALREADY_IN_PARTY, exception.getErrorCode());
        verify(partyRepository, never()).save(any(Party.class));
    }

    @Test
    void closeParty_리더가OPEN파티를마감하면CLOSED() {
        Party party = sampleParty("party-1", "leader", 4, true);
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(partyRepository.saveAndFlush(any(Party.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PartyStatusResponse response = taxiPartyService.closeParty("leader", "party-1");

        assertEquals(PartyStatus.CLOSED, response.status());
        assertEquals(PartyStatus.CLOSED, party.getStatus());
        verify(partySseService).publishPartyStatusChanged(party);
    }

    @Test
    void closeParty_리더가아니면_NOT_PARTY_LEADER() {
        Party party = sampleParty("party-1", "leader", 4, true);
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> taxiPartyService.closeParty("not-leader", "party-1")
        );

        assertEquals(ErrorCode.NOT_PARTY_LEADER, exception.getErrorCode());
        verify(partyRepository, never()).saveAndFlush(any(Party.class));
    }

    @Test
    void updateParty_OPEN에서_출발시간상세수정성공() {
        Party party = sampleParty("party-1", "leader", 4, true);
        LocalDateTime changed = LocalDateTime.now().plusHours(2);
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(partyRepository.saveAndFlush(any(Party.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(memberRepository.findAllById(any())).thenReturn(List.of());

        PartyDetailResponse response = taxiPartyService.updateParty(
                "leader",
                "party-1",
                new UpdatePartyRequest(changed, "변경 상세")
        );

        assertEquals(changed, response.departureTime());
        assertEquals("변경 상세", response.detail());
        assertEquals(PartyStatus.OPEN, response.status());
        verify(partySseService).publishPartyUpdated(party, null);
    }

    @Test
    void updateParty_CLOSED에서_시간수정해도상태유지() {
        Party party = sampleParty("party-1", "leader", 4, true);
        party.close();
        LocalDateTime changed = LocalDateTime.now().plusHours(3);
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(partyRepository.saveAndFlush(any(Party.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(memberRepository.findAllById(any())).thenReturn(List.of());

        PartyDetailResponse response = taxiPartyService.updateParty(
                "leader",
                "party-1",
                new UpdatePartyRequest(changed, null)
        );

        assertEquals(changed, response.departureTime());
        assertEquals(PartyStatus.CLOSED, response.status());
        assertEquals(PartyStatus.CLOSED, party.getStatus());
    }

    @Test
    void updateParty_detail만수정해도_성공() {
        Party party = sampleParty("party-1", "leader", 4, true);
        LocalDateTime originalTime = party.getDepartureTime();
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(partyRepository.saveAndFlush(any(Party.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(memberRepository.findAllById(any())).thenReturn(List.of());

        PartyDetailResponse response = taxiPartyService.updateParty(
                "leader",
                "party-1",
                new UpdatePartyRequest(null, "상세만 변경")
        );

        assertEquals(originalTime, response.departureTime());
        assertEquals("상세만 변경", response.detail());
        assertEquals("상세만 변경", party.getDetail());
    }

    @Test
    void updateParty_리더가아니면_NOT_PARTY_LEADER() {
        Party party = sampleParty("party-1", "leader", 4, true);
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> taxiPartyService.updateParty("not-leader", "party-1", new UpdatePartyRequest(LocalDateTime.now().plusHours(2), null))
        );

        assertEquals(ErrorCode.NOT_PARTY_LEADER, exception.getErrorCode());
    }

    @Test
    void updateParty_ARRIVED상태면_INVALID_PARTY_STATE_TRANSITION() {
        Party party = sampleParty("party-1", "leader", 4, true);
        party.arrive(14000);
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> taxiPartyService.updateParty("leader", "party-1", new UpdatePartyRequest(LocalDateTime.now().plusHours(2), null))
        );

        assertEquals(ErrorCode.INVALID_PARTY_STATE_TRANSITION, exception.getErrorCode());
    }

    @Test
    void updateParty_ENDED상태면_INVALID_PARTY_STATE_TRANSITION() {
        Party party = sampleParty("party-1", "leader", 4, true);
        party.cancel();
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> taxiPartyService.updateParty("leader", "party-1", new UpdatePartyRequest(LocalDateTime.now().plusHours(2), null))
        );

        assertEquals(ErrorCode.INVALID_PARTY_STATE_TRANSITION, exception.getErrorCode());
    }

    @Test
    void updateParty_수정필드없으면_VALIDATION_ERROR() {
        Party party = sampleParty("party-1", "leader", 4, true);
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> taxiPartyService.updateParty("leader", "party-1", new UpdatePartyRequest(null, null))
        );

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
    }

    @Test
    void updateParty_낙관적락충돌이면_PARTY_CONCURRENT_MODIFICATION() {
        Party party = sampleParty("party-1", "leader", 4, true);
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(partyRepository.saveAndFlush(any(Party.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Party.class, "party-1"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> taxiPartyService.updateParty("leader", "party-1", new UpdatePartyRequest(LocalDateTime.now().plusHours(2), "수정"))
        );

        assertEquals(ErrorCode.PARTY_CONCURRENT_MODIFICATION, exception.getErrorCode());
    }

    @Test
    void arriveParty_정산대상이없으면_NO_MEMBERS_TO_SETTLE() {
        Party party = sampleParty("party-1", "leader", 4, false);
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> taxiPartyService.arriveParty("leader", "party-1", 12000)
        );

        assertEquals(ErrorCode.NO_MEMBERS_TO_SETTLE, exception.getErrorCode());
    }

    @Test
    void createJoinRequest_정상생성() {
        Party party = sampleParty("party-1", "leader", 4, false);
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(memberRepository.findActiveByIdForUpdate("requester-1")).thenReturn(Optional.of(member("requester-1")));
        when(partyRepository.existsActivePartyByMemberId(eq("requester-1"), anySet(), isNull())).thenReturn(false);
        when(joinRequestRepository.existsByParty_IdAndRequesterIdAndStatus("party-1", "requester-1", JoinRequestStatus.PENDING))
                .thenReturn(false);
        when(joinRequestRepository.save(any(JoinRequest.class))).thenAnswer(invocation -> {
            JoinRequest saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", "request-1");
            return saved;
        });

        JoinRequestResponse response = taxiPartyService.createJoinRequest("requester-1", "party-1");

        assertEquals("request-1", response.id());
        assertEquals(JoinRequestStatus.PENDING, response.status());
        verify(joinRequestSseService).publishJoinRequestCreated(any(JoinRequest.class));
    }

    @Test
    void createJoinRequest_중복요청이면_ALREADY_REQUESTED() {
        Party party = sampleParty("party-1", "leader", 4, false);
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(memberRepository.findActiveByIdForUpdate("requester-1")).thenReturn(Optional.of(member("requester-1")));
        when(partyRepository.existsActivePartyByMemberId(eq("requester-1"), anySet(), isNull())).thenReturn(false);
        when(joinRequestRepository.existsByParty_IdAndRequesterIdAndStatus("party-1", "requester-1", JoinRequestStatus.PENDING))
                .thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> taxiPartyService.createJoinRequest("requester-1", "party-1")
        );

        assertEquals(ErrorCode.ALREADY_REQUESTED, exception.getErrorCode());
    }

    @Test
    void createJoinRequest_이전취소이력이있어도_재요청가능() {
        Party party = sampleParty("party-1", "leader", 4, false);
        JoinRequest canceled = JoinRequest.create(party, "requester-1");
        canceled.cancel();

        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(memberRepository.findActiveByIdForUpdate("requester-1")).thenReturn(Optional.of(member("requester-1")));
        when(partyRepository.existsActivePartyByMemberId(eq("requester-1"), anySet(), isNull())).thenReturn(false);
        when(joinRequestRepository.existsByParty_IdAndRequesterIdAndStatus("party-1", "requester-1", JoinRequestStatus.PENDING))
                .thenReturn(false);
        when(joinRequestRepository.save(any(JoinRequest.class))).thenAnswer(invocation -> {
            JoinRequest saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", "request-2");
            return saved;
        });

        JoinRequestResponse response = taxiPartyService.createJoinRequest("requester-1", "party-1");

        assertEquals("request-2", response.id());
        assertEquals(JoinRequestStatus.PENDING, response.status());
        assertEquals(JoinRequestStatus.CANCELED, canceled.getStatus());
    }

    @Test
    void acceptJoinRequest_정원도달시_자동CLOSED() {
        Party party = sampleParty("party-1", "leader", 2, false);
        JoinRequest joinRequest = JoinRequest.create(party, "requester-1");
        ReflectionTestUtils.setField(joinRequest, "id", "request-1");

        when(joinRequestRepository.findDetailById("request-1")).thenReturn(Optional.of(joinRequest));
        when(memberRepository.findActiveByIdForUpdate("requester-1")).thenReturn(Optional.of(member("requester-1")));
        when(joinRequestRepository.save(any(JoinRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(partyRepository.existsActivePartyByMemberId(eq("requester-1"), anySet(), eq("party-1"))).thenReturn(false);
        when(partyRepository.saveAndFlush(any(Party.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JoinRequestAcceptResponse response = taxiPartyService.acceptJoinRequest("leader", "request-1");

        assertEquals(JoinRequestStatus.ACCEPTED, response.status());
        assertEquals("party-1", response.partyId());
        assertEquals(2, party.getCurrentMembers());
        assertEquals(PartyStatus.CLOSED, party.getStatus());
        assertTrue(party.isMember("requester-1"));
        verify(chatService).syncPartyChatRoomMembers(party);
        verify(partySseService).publishPartyMemberJoined(party, "requester-1", null, party.getMemberIds());
        verify(joinRequestSseService).publishJoinRequestUpdated(joinRequest, JoinRequestStatus.PENDING);
        verify(partySseService).publishPartyStatusChanged(party);
    }

    @Test
    void declineJoinRequest_정상처리시_SSE업데이트발행() {
        Party party = sampleParty("party-1", "leader", 4, false);
        JoinRequest joinRequest = JoinRequest.create(party, "requester-1");
        ReflectionTestUtils.setField(joinRequest, "id", "request-1");
        when(joinRequestRepository.findDetailById("request-1")).thenReturn(Optional.of(joinRequest));
        when(joinRequestRepository.save(any(JoinRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JoinRequestResponse response = taxiPartyService.declineJoinRequest("leader", "request-1");

        assertEquals("request-1", response.id());
        assertEquals(JoinRequestStatus.DECLINED, response.status());
        verify(joinRequestSseService).publishJoinRequestUpdated(joinRequest, JoinRequestStatus.PENDING);
    }

    @Test
    void cancelJoinRequest_정상처리시_SSE업데이트발행() {
        Party party = sampleParty("party-1", "leader", 4, false);
        JoinRequest joinRequest = JoinRequest.create(party, "requester-1");
        ReflectionTestUtils.setField(joinRequest, "id", "request-1");
        when(joinRequestRepository.findDetailById("request-1")).thenReturn(Optional.of(joinRequest));
        when(joinRequestRepository.save(any(JoinRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JoinRequestResponse response = taxiPartyService.cancelJoinRequest("requester-1", "request-1");

        assertEquals("request-1", response.id());
        assertEquals(JoinRequestStatus.CANCELED, response.status());
        verify(joinRequestSseService).publishJoinRequestUpdated(joinRequest, JoinRequestStatus.PENDING);
    }

    @Test
    void declineJoinRequest_이전거절이력이있어도_새요청을다시거절할수있다() {
        Party party = sampleParty("party-1", "leader", 4, false);
        JoinRequest previousRequest = JoinRequest.create(party, "requester-1");
        previousRequest.decline();
        JoinRequest currentRequest = JoinRequest.create(party, "requester-1");
        ReflectionTestUtils.setField(currentRequest, "id", "request-2");

        when(joinRequestRepository.findDetailById("request-2")).thenReturn(Optional.of(currentRequest));
        when(joinRequestRepository.save(any(JoinRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JoinRequestResponse response = taxiPartyService.declineJoinRequest("leader", "request-2");

        assertEquals("request-2", response.id());
        assertEquals(JoinRequestStatus.DECLINED, response.status());
        assertEquals(JoinRequestStatus.DECLINED, previousRequest.getStatus());
    }

    @Test
    void cancelJoinRequest_이전취소이력이있어도_새요청을다시취소할수있다() {
        Party party = sampleParty("party-1", "leader", 4, false);
        JoinRequest previousRequest = JoinRequest.create(party, "requester-1");
        previousRequest.cancel();
        JoinRequest currentRequest = JoinRequest.create(party, "requester-1");
        ReflectionTestUtils.setField(currentRequest, "id", "request-2");

        when(joinRequestRepository.findDetailById("request-2")).thenReturn(Optional.of(currentRequest));
        when(joinRequestRepository.save(any(JoinRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JoinRequestResponse response = taxiPartyService.cancelJoinRequest("requester-1", "request-2");

        assertEquals("request-2", response.id());
        assertEquals(JoinRequestStatus.CANCELED, response.status());
        assertEquals(JoinRequestStatus.CANCELED, previousRequest.getStatus());
    }

    @Test
    void acceptJoinRequest_낙관적락충돌이면_PARTY_CONCURRENT_MODIFICATION() {
        Party party = sampleParty("party-1", "leader", 4, false);
        JoinRequest joinRequest = JoinRequest.create(party, "requester-1");
        ReflectionTestUtils.setField(joinRequest, "id", "request-1");

        when(joinRequestRepository.findDetailById("request-1")).thenReturn(Optional.of(joinRequest));
        when(memberRepository.findActiveByIdForUpdate("requester-1")).thenReturn(Optional.of(member("requester-1")));
        when(joinRequestRepository.save(any(JoinRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(partyRepository.existsActivePartyByMemberId(eq("requester-1"), anySet(), eq("party-1"))).thenReturn(false);
        when(partyRepository.saveAndFlush(any(Party.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Party.class, "party-1"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> taxiPartyService.acceptJoinRequest("leader", "request-1")
        );

        assertEquals(ErrorCode.PARTY_CONCURRENT_MODIFICATION, exception.getErrorCode());
    }

    @Test
    void leaveParty_일반멤버는_탈퇴성공() {
        Party party = sampleParty("party-1", "leader", 4, true);
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(partyRepository.saveAndFlush(any(Party.class))).thenAnswer(invocation -> invocation.getArgument(0));

        taxiPartyService.leaveParty("member-1", "party-1");

        assertFalse(party.isMember("member-1"));
        verify(chatService).syncPartyChatRoomMembers(party);
        verify(partySseService).publishPartyMemberLeft(party, "member-1", "LEFT", party.getMemberIds());
    }

    @Test
    void kickMember_강퇴당사자도_KICKED이벤트수신대상에포함() {
        Party party = sampleParty("party-1", "leader", 4, true);
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(partyRepository.saveAndFlush(any(Party.class))).thenAnswer(invocation -> invocation.getArgument(0));

        taxiPartyService.kickMember("leader", "party-1", "member-1");

        verify(chatService).syncPartyChatRoomMembers(party);
        verify(partySseService).publishPartyMemberLeft(
                eq(party),
                eq("member-1"),
                eq("KICKED"),
                argThat(recipients -> recipients.contains("member-1"))
        );
    }

    @Test
    void leaveParty_리더는_탈퇴불가() {
        Party party = sampleParty("party-1", "leader", 4, true);
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> taxiPartyService.leaveParty("leader", "party-1")
        );

        assertEquals(ErrorCode.LEADER_CANNOT_LEAVE, exception.getErrorCode());
    }

    @Test
    void confirmSettlement_마지막멤버확인시_정산만완료되고파티는ARRIVED유지() {
        Party party = sampleParty("party-1", "leader", 4, true);
        party.arrive(14000);

        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(partyRepository.saveAndFlush(any(Party.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SettlementConfirmResponse response = taxiPartyService.confirmSettlement("leader", "party-1", "member-1");

        assertTrue(response.allSettled());
        assertEquals(PartyStatus.ARRIVED, party.getStatus());
        assertEquals(SettlementStatus.COMPLETED, party.getSettlementStatus());
        verify(partySseService, never()).publishPartyStatusChanged(any(Party.class));
    }

    @Test
    void confirmSettlement_ARRIVED가아닌상태면_INVALID_PARTY_STATE_TRANSITION() {
        Party party = sampleParty("party-1", "leader", 4, true);
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> taxiPartyService.confirmSettlement("leader", "party-1", "member-1")
        );

        assertEquals(ErrorCode.INVALID_PARTY_STATE_TRANSITION, exception.getErrorCode());
    }

    @Test
    void cancelParty_리더가OPEN파티를취소하면_ENDED_CANCELLED() {
        Party party = sampleParty("party-1", "leader", 4, true);
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(partyRepository.saveAndFlush(any(Party.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PartyStatusResponse response = taxiPartyService.cancelParty("leader", "party-1");

        assertEquals(PartyStatus.ENDED, response.status());
        assertEquals(PartyEndReason.CANCELLED, response.endReason());
        assertEquals(PartyStatus.ENDED, party.getStatus());
        assertEquals(PartyEndReason.CANCELLED, party.getEndReason());
        verify(partySseService).publishPartyDeleted("party-1");
    }

    @Test
    void cancelParty_ARRIVED상태취소는_PARTY_NOT_CANCELABLE() {
        Party party = sampleParty("party-1", "leader", 4, true);
        party.arrive(14000);
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> taxiPartyService.cancelParty("leader", "party-1")
        );

        assertEquals(ErrorCode.PARTY_NOT_CANCELABLE, exception.getErrorCode());
    }

    @Test
    void validateWithdrawalAllowed_ARRIVED일반멤버면_탈퇴불가예외() {
        Party arrivedParty = sampleParty("party-1", "leader", 4, true);
        arrivedParty.arrive(14000);
        when(partyRepository.findActiveDetailsByMemberId("member-1", java.util.EnumSet.of(PartyStatus.OPEN, PartyStatus.CLOSED, PartyStatus.ARRIVED)))
                .thenReturn(List.of(arrivedParty));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> taxiPartyService.validateWithdrawalAllowed("member-1")
        );

        assertEquals(ErrorCode.MEMBER_WITHDRAWAL_NOT_ALLOWED, exception.getErrorCode());
    }

    @Test
    void handleMemberWithdrawal_리더탈퇴면_WITHDRAWED종료와대기요청거절을수행한다() {
        Party party = sampleParty("party-1", "leader", 4, true);
        JoinRequest joinRequest = JoinRequest.create(party, "requester-1");
        ReflectionTestUtils.setField(joinRequest, "id", "request-1");

        when(partyRepository.findActiveDetailsByMemberId("leader", java.util.EnumSet.of(PartyStatus.OPEN, PartyStatus.CLOSED, PartyStatus.ARRIVED)))
                .thenReturn(List.of(party));
        when(partyRepository.saveAndFlush(any(Party.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(joinRequestRepository.findByParty_IdAndStatusOrderByCreatedAtDesc("party-1", JoinRequestStatus.PENDING))
                .thenReturn(List.of(joinRequest));
        when(joinRequestRepository.save(any(JoinRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        taxiPartyService.handleMemberWithdrawal("leader");

        assertEquals(PartyStatus.ENDED, party.getStatus());
        assertEquals(PartyEndReason.WITHDRAWED, party.getEndReason());
        assertEquals(JoinRequestStatus.DECLINED, joinRequest.getStatus());
        verify(partySseService).publishPartyStatusChanged(party);
        verify(joinRequestSseService).publishJoinRequestUpdated(joinRequest, JoinRequestStatus.PENDING);
    }

    private CreatePartyRequest createPartyRequest(int maxMembers) {
        return new CreatePartyRequest(
                new PartyLocationRequest("성결대학교", 37.38, 126.93),
                new PartyLocationRequest("안양역", 37.40, 126.92),
                LocalDateTime.now().plusHours(1),
                maxMembers,
                List.of("빠른출발"),
                "택시비 나눠요"
        );
    }

    private Party sampleParty(String partyId, String leaderId, int maxMembers, boolean includeMember) {
        Party party = Party.create(
                leaderId,
                Location.of("성결대학교", 37.38, 126.93),
                Location.of("안양역", 37.40, 126.92),
                LocalDateTime.now().plusHours(1),
                maxMembers,
                List.of("빠른출발"),
                "택시비 나눠요"
        );
        ReflectionTestUtils.setField(party, "id", partyId);

        if (includeMember) {
            party.addMember("member-1");
        }
        return party;
    }

    private Member member(String memberId) {
        return Member.create(memberId, memberId + "@sungkyul.ac.kr", memberId, LocalDateTime.now());
    }
}
