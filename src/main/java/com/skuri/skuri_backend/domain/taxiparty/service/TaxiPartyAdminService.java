package com.skuri.skuri_backend.domain.taxiparty.service;

import com.skuri.skuri_backend.common.dto.PageResponse;
import com.skuri.skuri_backend.common.event.AfterCommitApplicationEventPublisher;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomRepository;
import com.skuri.skuri_backend.domain.chat.service.ChatService;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.notification.event.NotificationDomainEvent;
import com.skuri.skuri_backend.domain.taxiparty.constant.AdminPartyStatusAction;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.AdminPartyDetailResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.AdminPartyLeaderResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.AdminPartySummaryResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.MemberSettlementResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyLocationResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyMemberResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyStatusResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.SettlementAccountResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.SettlementSummaryResponse;
import com.skuri.skuri_backend.domain.taxiparty.entity.JoinRequestStatus;
import com.skuri.skuri_backend.domain.taxiparty.entity.Location;
import com.skuri.skuri_backend.domain.taxiparty.entity.MemberSettlement;
import com.skuri.skuri_backend.domain.taxiparty.entity.Party;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus;
import com.skuri.skuri_backend.domain.taxiparty.exception.PartyNotFoundException;
import com.skuri.skuri_backend.domain.taxiparty.repository.JoinRequestRepository;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyRepository;
import com.skuri.skuri_backend.infra.admin.list.AdminPageRequestPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TaxiPartyAdminService {

    private static final Sort ADMIN_PARTY_DEFAULT_SORT = Sort.by(
            Sort.Order.desc("departureTime"),
            Sort.Order.desc("createdAt")
    );

    private final PartyRepository partyRepository;
    private final JoinRequestRepository joinRequestRepository;
    private final MemberRepository memberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatService chatService;
    private final PartySseService partySseService;
    private final AfterCommitApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public PageResponse<AdminPartySummaryResponse> getAdminParties(
            PartyStatus status,
            LocalDate departureDate,
            String query,
            int page,
            int size
    ) {
        Pageable pageable = resolvePageable(page, size);
        Page<Party> partyPage = partyRepository.searchAdminParties(
                status,
                departureDate,
                normalizeQuery(query),
                pageable
        );

        Map<String, Party> detailedPartyMap = getDetailedPartyMap(partyPage.getContent());
        Map<String, Member> leaderMap = getMemberMap(
                detailedPartyMap.values().stream()
                        .map(Party::getLeaderId)
                        .distinct()
                        .toList()
        );

        return PageResponse.from(partyPage.map(party -> toAdminPartySummaryResponse(
                detailedPartyMap.getOrDefault(party.getId(), party),
                leaderMap
        )));
    }

    @Transactional(readOnly = true)
    public AdminPartyDetailResponse getAdminParty(String partyId) {
        Party party = partyRepository.findDetailById(partyId)
                .orElseThrow(PartyNotFoundException::new);
        Map<String, Member> memberMap = getMemberMap(party.getMemberIds());
        long pendingJoinRequestCount = joinRequestRepository.countByParty_IdAndStatus(partyId, JoinRequestStatus.PENDING);
        return toAdminPartyDetailResponse(party, memberMap, pendingJoinRequestCount);
    }

    @Transactional
    public PartyStatusResponse updatePartyStatus(String partyId, AdminPartyStatusAction action) {
        Party party = partyRepository.findDetailById(partyId)
                .orElseThrow(PartyNotFoundException::new);
        PartyStatus beforeStatus = party.getStatus();
        String partyChatActorId = party.getLeaderId();

        switch (action) {
            case CLOSE -> {
                party.close();
                savePartyWithLockHandling(party);
                chatService.createPartySystemMessage(party, partyChatActorId, "모집이 마감되었어요.");
                partySseService.publishPartyStatusChanged(party);
            }
            case REOPEN -> {
                party.reopen();
                savePartyWithLockHandling(party);
                chatService.createPartySystemMessage(party, partyChatActorId, "모집이 재개되었어요.");
                partySseService.publishPartyStatusChanged(party);
            }
            case CANCEL -> {
                party.cancel();
                savePartyWithLockHandling(party);
                chatService.createPartyEndMessage(party, partyChatActorId);
                partySseService.publishPartyDeleted(party.getId());
            }
            case END -> {
                party.forceEnd();
                savePartyWithLockHandling(party);
                chatService.createPartyEndMessage(party, partyChatActorId);
                partySseService.publishPartyStatusChanged(party);
            }
        }

        eventPublisher.publish(new NotificationDomainEvent.PartyStatusChanged(
                party.getId(),
                beforeStatus,
                party.getStatus()
        ));
        return new PartyStatusResponse(party.getId(), party.getStatus(), party.getEndReason());
    }

    private Pageable resolvePageable(int page, int size) {
        Pageable validated = AdminPageRequestPolicy.of(page, size);
        return PageRequest.of(validated.getPageNumber(), validated.getPageSize(), ADMIN_PARTY_DEFAULT_SORT);
    }

    private String normalizeQuery(String query) {
        if (!StringUtils.hasText(query)) {
            return null;
        }
        return query.trim();
    }

    private Map<String, Party> getDetailedPartyMap(List<Party> parties) {
        if (parties.isEmpty()) {
            return Map.of();
        }
        return partyRepository.findDetailsByIds(
                        parties.stream()
                                .map(Party::getId)
                                .toList()
                ).stream()
                .collect(LinkedHashMap::new, (map, party) -> map.put(party.getId(), party), Map::putAll);
    }

    private Map<String, Member> getMemberMap(Collection<String> memberIds) {
        List<String> normalizedMemberIds = memberIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (normalizedMemberIds.isEmpty()) {
            return Map.of();
        }
        return memberRepository.findAllById(normalizedMemberIds).stream()
                .collect(LinkedHashMap::new, (map, member) -> map.put(member.getId(), member), Map::putAll);
    }

    private AdminPartySummaryResponse toAdminPartySummaryResponse(Party party, Map<String, Member> leaderMap) {
        Member leader = leaderMap.get(party.getLeaderId());
        return new AdminPartySummaryResponse(
                party.getId(),
                party.getStatus(),
                party.getLeaderId(),
                leader != null ? leader.getNickname() : null,
                routeSummary(party),
                party.getDepartureTime(),
                party.getCurrentMembers(),
                party.getMaxMembers(),
                party.getCreatedAt()
        );
    }

    private AdminPartyDetailResponse toAdminPartyDetailResponse(
            Party party,
            Map<String, Member> memberMap,
            long pendingJoinRequestCount
    ) {
        Member leaderProfile = memberMap.get(party.getLeaderId());
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

        return new AdminPartyDetailResponse(
                party.getId(),
                party.getStatus(),
                party.getEndReason(),
                party.getLeaderId(),
                leaderProfile != null ? leaderProfile.getNickname() : null,
                leaderProfile == null
                        ? null
                        : new AdminPartyLeaderResponse(
                                leaderProfile.getId(),
                                leaderProfile.getNickname(),
                                leaderProfile.getPhotoUrl()
                        ),
                routeSummary(party),
                toLocationResponse(party.getDeparture()),
                toLocationResponse(party.getDestination()),
                party.getDepartureTime(),
                party.getCurrentMembers(),
                party.getMaxMembers(),
                members,
                party.getTagsText(),
                party.getDetail(),
                pendingJoinRequestCount,
                party.getSettlementStatus(),
                toSettlementSummary(party, memberMap),
                resolveChatRoomId(party.getId()),
                party.getCreatedAt(),
                party.getUpdatedAt(),
                party.getEndedAt()
        );
    }

    private String resolveChatRoomId(String partyId) {
        String chatRoomId = "party:" + partyId;
        return chatRoomRepository.existsById(chatRoomId) ? chatRoomId : null;
    }

    private String routeSummary(Party party) {
        return party.getDeparture().getName() + " -> " + party.getDestination().getName();
    }

    private PartyLocationResponse toLocationResponse(Location location) {
        if (location == null) {
            return null;
        }
        return new PartyLocationResponse(location.getName(), location.getLat(), location.getLng());
    }

    private SettlementSummaryResponse toSettlementSummary(Party party, Map<String, Member> memberMap) {
        if (party.getSettlementStatus() == null) {
            return null;
        }

        return new SettlementSummaryResponse(
                party.getSettlementStatus(),
                party.getTaxiFare(),
                party.getSplitMemberCount(),
                party.getPerPersonAmount(),
                party.getSettlementTargetMemberIds(),
                toSettlementAccountResponse(party),
                party.getSettlementItems().stream()
                        .map(item -> toMemberSettlementResponse(item, memberMap))
                        .toList()
        );
    }

    private MemberSettlementResponse toMemberSettlementResponse(MemberSettlement settlement, Map<String, Member> memberMap) {
        Member member = memberMap.get(settlement.getMemberId());
        String displayName = settlement.getDisplayName();
        if (!StringUtils.hasText(displayName) && member != null) {
            displayName = member.getNickname();
        }
        return new MemberSettlementResponse(
                settlement.getMemberId(),
                displayName,
                settlement.isSettled(),
                settlement.getSettledAt(),
                settlement.isLeftParty(),
                settlement.getLeftAt()
        );
    }

    private SettlementAccountResponse toSettlementAccountResponse(Party party) {
        if (party.getSettlementAccount() == null) {
            return null;
        }
        return new SettlementAccountResponse(
                party.getSettlementAccount().getBankName(),
                party.getSettlementAccount().getAccountNumber(),
                party.getSettlementAccount().getAccountHolder(),
                party.getSettlementAccount().getHideName()
        );
    }

    private void savePartyWithLockHandling(Party party) {
        try {
            partyRepository.saveAndFlush(party);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new BusinessException(ErrorCode.PARTY_CONCURRENT_MODIFICATION);
        }
    }
}
