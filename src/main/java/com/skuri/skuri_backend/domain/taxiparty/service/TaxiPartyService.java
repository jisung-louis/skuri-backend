package com.skuri.skuri_backend.domain.taxiparty.service;

import com.skuri.skuri_backend.common.dto.PageResponse;
import com.skuri.skuri_backend.common.event.AfterCommitApplicationEventPublisher;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.chat.service.ChatService;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.exception.MemberNotFoundException;
import com.skuri.skuri_backend.domain.member.exception.MemberWithdrawalNotAllowedException;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.notification.event.NotificationDomainEvent;
import com.skuri.skuri_backend.domain.taxiparty.dto.request.CreatePartyRequest;
import com.skuri.skuri_backend.domain.taxiparty.dto.request.UpdatePartyRequest;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.JoinRequestListItemResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.JoinRequestAcceptResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.JoinRequestResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.MemberSettlementResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.MyPartyResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyCreateResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyDetailResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyLocationResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyMemberResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyStatusResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartySummaryResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.SettlementConfirmResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.SettlementSummaryResponse;
import com.skuri.skuri_backend.domain.taxiparty.entity.JoinRequest;
import com.skuri.skuri_backend.domain.taxiparty.entity.JoinRequestStatus;
import com.skuri.skuri_backend.domain.taxiparty.entity.Location;
import com.skuri.skuri_backend.domain.taxiparty.entity.MemberSettlement;
import com.skuri.skuri_backend.domain.taxiparty.entity.Party;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyMember;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus;
import com.skuri.skuri_backend.domain.taxiparty.exception.JoinRequestNotFoundException;
import com.skuri.skuri_backend.domain.taxiparty.exception.PartyNotFoundException;
import com.skuri.skuri_backend.domain.taxiparty.repository.JoinRequestRepository;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TaxiPartyService {

    private static final Set<PartyStatus> ACTIVE_PARTY_STATUSES = EnumSet.of(PartyStatus.OPEN, PartyStatus.CLOSED, PartyStatus.ARRIVED);

    private final PartyRepository partyRepository;
    private final JoinRequestRepository joinRequestRepository;
    private final MemberRepository memberRepository;
    private final ChatService chatService;
    private final PartySseService partySseService;
    private final JoinRequestSseService joinRequestSseService;
    private final AfterCommitApplicationEventPublisher eventPublisher;

    @Transactional
    public PartyCreateResponse createParty(String leaderId, CreatePartyRequest request) {
        lockMemberOrThrow(leaderId);
        if (partyRepository.existsActivePartyByMemberId(leaderId, ACTIVE_PARTY_STATUSES, null)) {
            throw new BusinessException(ErrorCode.ALREADY_IN_PARTY);
        }

        Party created = partyRepository.save(
                Party.create(
                        leaderId,
                        toLocation(request.departure()),
                        toLocation(request.destination()),
                        request.departureTime(),
                        request.maxMembers(),
                        request.tags(),
                        request.detail()
                )
        );
        chatService.createPartyChatRoom(created);
        Member leader = memberRepository.findById(leaderId).orElse(null);
        partySseService.publishPartyCreated(created, leader);
        eventPublisher.publish(new NotificationDomainEvent.PartyCreated(created.getId()));

        return new PartyCreateResponse(created.getId(), "party:" + created.getId());
    }

    @Transactional(readOnly = true)
    public PageResponse<PartySummaryResponse> getParties(
            PartyStatus status,
            LocalDateTime departureTime,
            String departureName,
            String destinationName,
            Pageable pageable
    ) {
        Page<Party> page = partyRepository.search(status, departureTime, departureName, destinationName, pageable);
        Map<String, Member> leaderMap = getMemberMap(page.getContent().stream().map(Party::getLeaderId).toList());

        return PageResponse.from(page.map(party -> toPartySummaryResponse(party, leaderMap.get(party.getLeaderId()))));
    }

    @Transactional(readOnly = true)
    public PartyDetailResponse getPartyDetail(String partyId) {
        Party party = findPartyDetailOrThrow(partyId);
        Map<String, Member> memberMap = getMemberMap(party.getMemberIds());
        return toPartyDetailResponse(party, memberMap);
    }

    @Transactional
    public PartyDetailResponse updateParty(String actorId, String partyId, UpdatePartyRequest request) {
        Party party = findPartyDetailOrThrow(partyId);
        requireLeader(party, actorId);

        if (party.getStatus() != PartyStatus.OPEN && party.getStatus() != PartyStatus.CLOSED) {
            throw new BusinessException(ErrorCode.INVALID_PARTY_STATE_TRANSITION, "OPEN/CLOSED 상태에서만 수정할 수 있습니다.");
        }
        if (request.departureTime() == null && request.detail() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "departureTime 또는 detail 중 최소 하나는 입력해야 합니다.");
        }

        if (request.departureTime() != null) {
            party.updateDepartureTime(request.departureTime());
        }
        if (request.detail() != null) {
            party.updateDetail(request.detail());
        }

        savePartyWithLockHandling(party);
        Map<String, Member> memberMap = getMemberMap(party.getMemberIds());
        partySseService.publishPartyUpdated(party, memberMap.get(party.getLeaderId()));
        return toPartyDetailResponse(party, memberMap);
    }

    @Transactional(readOnly = true)
    public List<MyPartyResponse> getMyParties(String memberId) {
        List<Party> parties = partyRepository.findMyParties(memberId);
        Map<String, Member> memberMap = getMemberMap(
                parties.stream()
                        .flatMap(party -> party.getMemberIds().stream())
                        .distinct()
                        .toList()
        );

        return parties.stream()
                .map(party -> new MyPartyResponse(
                        party.getId(),
                        party.getStatus(),
                        toLocationResponse(party.getDeparture()),
                        toLocationResponse(party.getDestination()),
                        party.isLeader(memberId),
                        toSettlementSummary(party, memberMap)
                ))
                .toList();
    }

    @Transactional
    public PartyStatusResponse closeParty(String actorId, String partyId) {
        Party party = findPartyDetailOrThrow(partyId);
        requireLeader(party, actorId);
        PartyStatus beforeStatus = party.getStatus();
        party.close();
        savePartyWithLockHandling(party);
        partySseService.publishPartyStatusChanged(party);
        eventPublisher.publish(new NotificationDomainEvent.PartyStatusChanged(party.getId(), beforeStatus, party.getStatus()));
        return toPartyStatusResponse(party);
    }

    @Transactional
    public PartyStatusResponse reopenParty(String actorId, String partyId) {
        Party party = findPartyDetailOrThrow(partyId);
        requireLeader(party, actorId);
        PartyStatus beforeStatus = party.getStatus();
        party.reopen();
        savePartyWithLockHandling(party);
        partySseService.publishPartyStatusChanged(party);
        eventPublisher.publish(new NotificationDomainEvent.PartyStatusChanged(party.getId(), beforeStatus, party.getStatus()));
        return toPartyStatusResponse(party);
    }

    @Transactional
    public PartyDetailResponse arriveParty(String actorId, String partyId, int taxiFare) {
        Party party = findPartyDetailOrThrow(partyId);
        requireLeader(party, actorId);
        PartyStatus beforeStatus = party.getStatus();
        party.arrive(taxiFare);
        savePartyWithLockHandling(party);
        partySseService.publishPartyStatusChanged(party);
        eventPublisher.publish(new NotificationDomainEvent.PartyStatusChanged(party.getId(), beforeStatus, party.getStatus()));

        Map<String, Member> memberMap = getMemberMap(party.getMemberIds());
        return toPartyDetailResponse(party, memberMap);
    }

    @Transactional
    public PartyStatusResponse endParty(String actorId, String partyId) {
        Party party = findPartyDetailOrThrow(partyId);
        requireLeader(party, actorId);
        PartyStatus beforeStatus = party.getStatus();
        party.forceEnd();
        savePartyWithLockHandling(party);
        partySseService.publishPartyStatusChanged(party);
        eventPublisher.publish(new NotificationDomainEvent.PartyStatusChanged(party.getId(), beforeStatus, party.getStatus()));
        return toPartyStatusResponse(party);
    }

    @Transactional
    public PartyStatusResponse cancelParty(String actorId, String partyId) {
        Party party = findPartyDetailOrThrow(partyId);
        requireLeader(party, actorId);
        PartyStatus beforeStatus = party.getStatus();
        party.cancel();
        savePartyWithLockHandling(party);
        partySseService.publishPartyDeleted(party.getId());
        eventPublisher.publish(new NotificationDomainEvent.PartyStatusChanged(party.getId(), beforeStatus, party.getStatus()));
        return toPartyStatusResponse(party);
    }

    @Transactional
    public void kickMember(String actorId, String partyId, String memberId) {
        Party party = findPartyDetailOrThrow(partyId);
        requireLeader(party, actorId);

        if (party.getStatus() == PartyStatus.ARRIVED) {
            throw new BusinessException(ErrorCode.CANNOT_KICK_IN_ARRIVED);
        }
        if (party.getStatus() == PartyStatus.ENDED) {
            throw new BusinessException(ErrorCode.PARTY_ENDED);
        }
        if (party.isLeader(memberId)) {
            throw new BusinessException(ErrorCode.CANNOT_KICK_LEADER);
        }

        List<String> recipientsBeforeRemoval = party.getMemberIds();
        party.removeMember(memberId);
        savePartyWithLockHandling(party);
        chatService.syncPartyChatRoomMembers(party);
        partySseService.publishPartyMemberLeft(party, memberId, "KICKED", recipientsBeforeRemoval);
        eventPublisher.publish(new NotificationDomainEvent.PartyMemberKicked(party.getId(), memberId));
    }

    @Transactional
    public void leaveParty(String memberId, String partyId) {
        Party party = findPartyDetailOrThrow(partyId);

        if (!party.isMember(memberId)) {
            throw new BusinessException(ErrorCode.NOT_PARTY_MEMBER);
        }
        if (party.isLeader(memberId)) {
            throw new BusinessException(ErrorCode.LEADER_CANNOT_LEAVE);
        }
        if (party.getStatus() == PartyStatus.ARRIVED) {
            throw new BusinessException(ErrorCode.CANNOT_LEAVE_ARRIVED_PARTY);
        }
        if (party.getStatus() == PartyStatus.ENDED) {
            throw new BusinessException(ErrorCode.PARTY_ENDED);
        }

        party.removeMember(memberId);
        savePartyWithLockHandling(party);
        chatService.syncPartyChatRoomMembers(party);
        partySseService.publishPartyMemberLeft(party, memberId, "LEFT", party.getMemberIds());
    }

    @Transactional
    public JoinRequestResponse createJoinRequest(String requesterId, String partyId) {
        Party party = findPartyDetailOrThrow(partyId);
        lockMemberOrThrow(requesterId);

        if (party.getStatus() == PartyStatus.ENDED) {
            throw new BusinessException(ErrorCode.PARTY_ENDED);
        }
        if (party.getStatus() != PartyStatus.OPEN) {
            throw new BusinessException(ErrorCode.PARTY_CLOSED);
        }
        if (party.isMember(requesterId)) {
            throw new BusinessException(ErrorCode.ALREADY_IN_PARTY);
        }
        if (partyRepository.existsActivePartyByMemberId(requesterId, ACTIVE_PARTY_STATUSES, null)) {
            throw new BusinessException(ErrorCode.ALREADY_IN_PARTY);
        }
        if (joinRequestRepository.existsByParty_IdAndRequesterIdAndStatus(partyId, requesterId, JoinRequestStatus.PENDING)) {
            throw new BusinessException(ErrorCode.ALREADY_REQUESTED);
        }

        JoinRequest joinRequest = joinRequestRepository.save(JoinRequest.create(party, requesterId));
        joinRequestSseService.publishJoinRequestCreated(joinRequest);
        eventPublisher.publish(new NotificationDomainEvent.PartyJoinRequestCreated(joinRequest.getId()));
        return toJoinRequestResponse(joinRequest);
    }

    @Transactional
    public JoinRequestAcceptResponse acceptJoinRequest(String leaderId, String requestId) {
        JoinRequest joinRequest = findJoinRequestOrThrow(requestId);
        Party party = joinRequest.getParty();
        JoinRequestStatus previousStatus = joinRequest.getStatus();
        PartyStatus beforeStatus = party.getStatus();
        requireJoinRequestLeader(joinRequest, leaderId);

        if (party.getStatus() == PartyStatus.ENDED) {
            throw new BusinessException(ErrorCode.PARTY_ENDED);
        }
        if (party.getStatus() != PartyStatus.OPEN) {
            throw new BusinessException(ErrorCode.PARTY_CLOSED);
        }

        String requesterId = joinRequest.getRequesterId();
        lockMemberOrThrow(requesterId);
        if (party.isMember(requesterId)) {
            throw new BusinessException(ErrorCode.ALREADY_IN_PARTY);
        }
        if (partyRepository.existsActivePartyByMemberId(requesterId, ACTIVE_PARTY_STATUSES, party.getId())) {
            throw new BusinessException(ErrorCode.ALREADY_IN_PARTY);
        }

        joinRequest.accept();
        party.addMember(requesterId);

        joinRequestRepository.save(joinRequest);
        savePartyWithLockHandling(party);
        chatService.syncPartyChatRoomMembers(party);
        String requesterName = memberRepository.findById(requesterId)
                .map(Member::getNickname)
                .orElse(null);
        partySseService.publishPartyMemberJoined(party, requesterId, requesterName, party.getMemberIds());
        joinRequestSseService.publishJoinRequestUpdated(joinRequest, previousStatus);
        if (beforeStatus != party.getStatus()) {
            partySseService.publishPartyStatusChanged(party);
            eventPublisher.publish(new NotificationDomainEvent.PartyStatusChanged(party.getId(), beforeStatus, party.getStatus()));
        }
        eventPublisher.publish(new NotificationDomainEvent.PartyJoinRequestProcessed(joinRequest.getId(), joinRequest.getStatus()));
        return toJoinRequestAcceptResponse(joinRequest);
    }

    @Transactional
    public JoinRequestResponse declineJoinRequest(String leaderId, String requestId) {
        JoinRequest joinRequest = findJoinRequestOrThrow(requestId);
        JoinRequestStatus previousStatus = joinRequest.getStatus();
        requireJoinRequestLeader(joinRequest, leaderId);
        joinRequest.decline();
        joinRequestRepository.save(joinRequest);
        joinRequestSseService.publishJoinRequestUpdated(joinRequest, previousStatus);
        eventPublisher.publish(new NotificationDomainEvent.PartyJoinRequestProcessed(joinRequest.getId(), joinRequest.getStatus()));
        return toJoinRequestResponse(joinRequest);
    }

    @Transactional
    public JoinRequestResponse cancelJoinRequest(String requesterId, String requestId) {
        JoinRequest joinRequest = findJoinRequestOrThrow(requestId);
        if (!joinRequest.isRequester(requesterId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "요청자 본인만 취소할 수 있습니다.");
        }

        JoinRequestStatus previousStatus = joinRequest.getStatus();
        joinRequest.cancel();
        joinRequestRepository.save(joinRequest);
        joinRequestSseService.publishJoinRequestUpdated(joinRequest, previousStatus);
        return toJoinRequestResponse(joinRequest);
    }

    @Transactional(readOnly = true)
    public List<JoinRequestListItemResponse> getPartyJoinRequests(String leaderId, String partyId) {
        Party party = findPartyDetailOrThrow(partyId);
        requireLeader(party, leaderId);

        List<JoinRequest> requests = joinRequestRepository.findByParty_IdOrderByCreatedAtDesc(partyId);
        return mapJoinRequestResponses(requests);
    }

    @Transactional(readOnly = true)
    public List<JoinRequestListItemResponse> getMyJoinRequests(String requesterId, JoinRequestStatus status) {
        List<JoinRequest> requests = status == null
                ? joinRequestRepository.findByRequesterIdOrderByCreatedAtDesc(requesterId)
                : joinRequestRepository.findByRequesterIdAndStatusOrderByCreatedAtDesc(requesterId, status);
        return mapJoinRequestResponses(requests);
    }

    @Transactional
    public SettlementConfirmResponse confirmSettlement(String leaderId, String partyId, String memberId) {
        Party party = findPartyDetailOrThrow(partyId);
        requireLeader(party, leaderId);

        boolean allSettled = party.confirmSettlement(memberId);
        savePartyWithLockHandling(party);
        if (allSettled) {
            eventPublisher.publish(new NotificationDomainEvent.PartySettlementCompleted(party.getId()));
        }

        MemberSettlement target = party.getSettlementItems().stream()
                .filter(item -> item.getMemberId().equals(memberId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_PARTY_MEMBER, "정산 대상 멤버가 아닙니다."));

        return new SettlementConfirmResponse(memberId, target.isSettled(), target.getSettledAt(), allSettled);
    }

    @Transactional(readOnly = true)
    public void validateWithdrawalAllowed(String memberId) {
        boolean joinedArrivedParty = partyRepository.findActiveDetailsByMemberId(memberId, ACTIVE_PARTY_STATUSES).stream()
                .anyMatch(party -> party.getStatus() == PartyStatus.ARRIVED && !party.isLeader(memberId));
        if (joinedArrivedParty) {
            throw new MemberWithdrawalNotAllowedException("정산이 진행 중인 ARRIVED 파티에 참여 중인 멤버는 탈퇴할 수 없습니다.");
        }
    }

    @Transactional
    public void handleMemberWithdrawal(String memberId) {
        List<Party> activeParties = partyRepository.findActiveDetailsByMemberId(memberId, ACTIVE_PARTY_STATUSES);
        for (Party party : activeParties) {
            if (party.isLeader(memberId)) {
                withdrawLeaderFromParty(party);
                continue;
            }

            if (party.getStatus() == PartyStatus.ARRIVED) {
                throw new MemberWithdrawalNotAllowedException("정산이 진행 중인 ARRIVED 파티에 참여 중인 멤버는 탈퇴할 수 없습니다.");
            }

            party.removeMember(memberId);
            savePartyWithLockHandling(party);
            chatService.syncPartyChatRoomMembers(party);
            partySseService.publishPartyMemberLeft(party, memberId, "WITHDRAWN", party.getMemberIds());
        }

        joinRequestRepository.findByRequesterIdAndStatusOrderByCreatedAtDesc(memberId, JoinRequestStatus.PENDING)
                .forEach(request -> {
                    JoinRequestStatus previousStatus = request.getStatus();
                    request.cancel();
                    joinRequestRepository.save(request);
                    joinRequestSseService.publishJoinRequestUpdated(request, previousStatus);
                });
    }

    private List<JoinRequestListItemResponse> mapJoinRequestResponses(List<JoinRequest> requests) {
        Map<String, Member> requesterMap = getMemberMap(requests.stream().map(JoinRequest::getRequesterId).toList());

        return requests.stream()
                .map(request -> {
                    Member requester = requesterMap.get(request.getRequesterId());
                    return new JoinRequestListItemResponse(
                            request.getId(),
                            request.getParty().getId(),
                            request.getRequesterId(),
                            requester != null ? requester.getNickname() : null,
                            requester != null ? requester.getPhotoUrl() : null,
                            request.getStatus(),
                            request.getCreatedAt()
                    );
                })
                .toList();
    }

    private Party findPartyDetailOrThrow(String partyId) {
        return partyRepository.findDetailById(partyId)
                .orElseThrow(PartyNotFoundException::new);
    }

    private JoinRequest findJoinRequestOrThrow(String requestId) {
        return joinRequestRepository.findDetailById(requestId)
                .orElseThrow(JoinRequestNotFoundException::new);
    }

    private void lockMemberOrThrow(String memberId) {
        memberRepository.findActiveByIdForUpdate(memberId)
                .orElseThrow(MemberNotFoundException::new);
    }

    private void withdrawLeaderFromParty(Party party) {
        PartyStatus beforeStatus = party.getStatus();
        party.withdrawLeader();
        savePartyWithLockHandling(party);
        partySseService.publishPartyStatusChanged(party);
        eventPublisher.publish(new NotificationDomainEvent.PartyStatusChanged(party.getId(), beforeStatus, party.getStatus()));

        joinRequestRepository.findByParty_IdAndStatusOrderByCreatedAtDesc(party.getId(), JoinRequestStatus.PENDING)
                .forEach(request -> {
                    JoinRequestStatus previousStatus = request.getStatus();
                    request.decline();
                    joinRequestRepository.save(request);
                    joinRequestSseService.publishJoinRequestUpdated(request, previousStatus);
                });
    }

    private void requireLeader(Party party, String actorId) {
        if (!party.isLeader(actorId)) {
            throw new BusinessException(ErrorCode.NOT_PARTY_LEADER);
        }
    }

    private void requireJoinRequestLeader(JoinRequest request, String leaderId) {
        if (!request.getLeaderId().equals(leaderId)) {
            throw new BusinessException(ErrorCode.NOT_PARTY_LEADER);
        }
    }

    private void savePartyWithLockHandling(Party party) {
        try {
            partyRepository.saveAndFlush(party);
        } catch (ObjectOptimisticLockingFailureException | OptimisticLockException e) {
            throw new BusinessException(ErrorCode.PARTY_CONCURRENT_MODIFICATION);
        }
    }

    private PartySummaryResponse toPartySummaryResponse(Party party, Member leader) {
        return new PartySummaryResponse(
                party.getId(),
                party.getLeaderId(),
                leader != null ? leader.getNickname() : null,
                leader != null ? leader.getPhotoUrl() : null,
                toLocationResponse(party.getDeparture()),
                toLocationResponse(party.getDestination()),
                party.getDepartureTime(),
                party.getMaxMembers(),
                party.getCurrentMembers(),
                party.getTagsText(),
                party.getDetail(),
                party.getStatus(),
                party.getCreatedAt()
        );
    }

    private PartyDetailResponse toPartyDetailResponse(Party party, Map<String, Member> memberMap) {
        Member leader = memberMap.get(party.getLeaderId());

        List<PartyMemberResponse> members = party.getMembers().stream()
                .map(member -> {
                    Member profile = memberMap.get(member.getMemberId());
                    return new PartyMemberResponse(
                            member.getMemberId(),
                            profile != null ? profile.getNickname() : null,
                            profile != null ? profile.getPhotoUrl() : null,
                            party.isLeader(member.getMemberId()),
                            member.getJoinedAt()
                    );
                })
                .toList();

        return new PartyDetailResponse(
                party.getId(),
                party.getLeaderId(),
                leader != null ? leader.getNickname() : null,
                leader != null ? leader.getPhotoUrl() : null,
                toLocationResponse(party.getDeparture()),
                toLocationResponse(party.getDestination()),
                party.getDepartureTime(),
                party.getMaxMembers(),
                members,
                party.getTagsText(),
                party.getDetail(),
                party.getStatus(),
                toSettlementSummary(party, memberMap),
                party.getCreatedAt()
        );
    }

    private SettlementSummaryResponse toSettlementSummary(Party party, Map<String, Member> memberMap) {
        if (party.getSettlementStatus() == null) {
            return null;
        }

        List<MemberSettlementResponse> settlements = party.getSettlementItems().stream()
                .sorted(Comparator.comparing(MemberSettlement::getMemberId))
                .map(item -> {
                    Member profile = memberMap.get(item.getMemberId());
                    return new MemberSettlementResponse(
                            item.getMemberId(),
                            profile != null ? profile.getNickname() : null,
                            item.isSettled(),
                            item.getSettledAt()
                    );
                })
                .toList();

        return new SettlementSummaryResponse(party.getSettlementStatus(), party.getPerPersonAmount(), settlements);
    }

    private PartyStatusResponse toPartyStatusResponse(Party party) {
        return new PartyStatusResponse(party.getId(), party.getStatus(), party.getEndReason());
    }

    private JoinRequestResponse toJoinRequestResponse(JoinRequest joinRequest) {
        return new JoinRequestResponse(joinRequest.getId(), joinRequest.getStatus());
    }

    private JoinRequestAcceptResponse toJoinRequestAcceptResponse(JoinRequest joinRequest) {
        return new JoinRequestAcceptResponse(joinRequest.getId(), joinRequest.getStatus(), joinRequest.getParty().getId());
    }

    private Location toLocation(com.skuri.skuri_backend.domain.taxiparty.dto.request.PartyLocationRequest request) {
        return Location.of(request.name(), request.lat(), request.lng());
    }

    private PartyLocationResponse toLocationResponse(Location location) {
        return new PartyLocationResponse(location.getName(), location.getLat(), location.getLng());
    }

    private Map<String, Member> getMemberMap(Collection<String> memberIds) {
        List<String> ids = memberIds.stream().distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }

        Map<String, Member> result = new HashMap<>();
        memberRepository.findAllById(ids).forEach(member -> result.put(member.getId(), member));
        return result;
    }
}
