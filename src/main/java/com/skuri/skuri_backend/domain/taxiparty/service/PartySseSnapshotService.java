package com.skuri.skuri_backend.domain.taxiparty.service;

import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyLocationResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyParticipantSummaryResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartySummaryResponse;
import com.skuri.skuri_backend.domain.taxiparty.entity.Party;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyRepository;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartySseSnapshotService {

    private final PartyRepository partyRepository;
    private final MemberRepository memberRepository;
    private final PartyTagRepository partyTagRepository;

    public Map<String, Object> createSnapshotPayload() {
        return Map.of("parties", getSnapshotParties());
    }

    public PartySummaryResponse toPartySummaryResponse(Party party, Member leader) {
        Map<String, Member> memberMap = getMemberMap(party.getMemberIds());
        if (leader != null) {
            memberMap.put(leader.getId(), leader);
        }
        return toPartySummaryResponse(party, memberMap, party.getTagsText());
    }

    private PartySummaryResponse toPartySummaryResponse(
            Party party,
            Map<String, Member> memberMap,
            List<String> tags
    ) {
        Member leader = memberMap.get(party.getLeaderId());
        return new PartySummaryResponse(
                party.getId(),
                party.getLeaderId(),
                leader != null ? leader.getNickname() : null,
                leader != null ? leader.getPhotoUrl() : null,
                toParticipantSummaries(party, memberMap),
                new PartyLocationResponse(party.getDeparture().getName(), party.getDeparture().getLat(), party.getDeparture().getLng()),
                new PartyLocationResponse(party.getDestination().getName(), party.getDestination().getLat(), party.getDestination().getLng()),
                party.getDepartureTime(),
                party.getMaxMembers(),
                party.getCurrentMembers(),
                tags,
                party.getDetail(),
                party.getStatus(),
                party.getCreatedAt()
        );
    }

    private List<PartySummaryResponse> getSnapshotParties() {
        List<Party> parties = partyRepository.findSseSnapshotParties();
        Map<String, List<String>> partyTagMap = getPartyTagMap(
                parties.stream()
                        .map(Party::getId)
                        .toList()
        );
        Map<String, Member> memberMap = getMemberMap(
                parties.stream()
                        .flatMap(party -> party.getMemberIds().stream())
                        .toList()
        );

        return parties.stream()
                .map(party -> toPartySummaryResponse(
                        party,
                        memberMap,
                        partyTagMap.getOrDefault(party.getId(), List.of())
                ))
                .toList();
    }

    private List<PartyParticipantSummaryResponse> toParticipantSummaries(Party party, Map<String, Member> memberMap) {
        return party.getMembers().stream()
                .map(member -> {
                    Member profile = memberMap.get(member.getMemberId());
                    return new PartyParticipantSummaryResponse(
                            member.getMemberId(),
                            profile != null ? profile.getPhotoUrl() : null,
                            profile != null ? profile.getNickname() : null,
                            party.isLeader(member.getMemberId())
                    );
                })
                .toList();
    }

    private Map<String, Member> getMemberMap(List<String> memberIds) {
        List<String> ids = memberIds.stream().distinct().toList();
        if (ids.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, Member> result = new HashMap<>();
        memberRepository.findAllById(ids).forEach(member -> result.put(member.getId(), member));
        return result;
    }

    private Map<String, List<String>> getPartyTagMap(List<String> partyIds) {
        List<String> ids = partyIds.stream().distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }

        Map<String, List<String>> result = new HashMap<>();
        partyTagRepository.findTagSummariesByPartyIds(ids).forEach(tagSummary ->
                result.computeIfAbsent(tagSummary.getPartyId(), unused -> new ArrayList<>())
                        .add(tagSummary.getTag())
        );
        return result;
    }
}
