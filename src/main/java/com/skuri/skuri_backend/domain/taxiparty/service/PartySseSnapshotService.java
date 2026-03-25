package com.skuri.skuri_backend.domain.taxiparty.service;

import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyLocationResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartySummaryResponse;
import com.skuri.skuri_backend.domain.taxiparty.entity.Party;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartySseSnapshotService {

    private final PartyRepository partyRepository;
    private final MemberRepository memberRepository;

    public Map<String, Object> createSnapshotPayload() {
        return Map.of("parties", getSnapshotParties());
    }

    public PartySummaryResponse toPartySummaryResponse(Party party, Member leader) {
        return new PartySummaryResponse(
                party.getId(),
                party.getLeaderId(),
                leader != null ? leader.getNickname() : null,
                leader != null ? leader.getPhotoUrl() : null,
                new PartyLocationResponse(party.getDeparture().getName(), party.getDeparture().getLat(), party.getDeparture().getLng()),
                new PartyLocationResponse(party.getDestination().getName(), party.getDestination().getLat(), party.getDestination().getLng()),
                party.getDepartureTime(),
                party.getMaxMembers(),
                party.getCurrentMembers(),
                party.getTagsText(),
                party.getDetail(),
                party.getStatus(),
                party.getCreatedAt()
        );
    }

    private List<PartySummaryResponse> getSnapshotParties() {
        List<Party> parties = partyRepository.findSseSnapshotParties();
        List<String> leaderIds = parties.stream().map(Party::getLeaderId).distinct().toList();
        Map<String, Member> leaderMap = new HashMap<>();
        memberRepository.findAllById(leaderIds).forEach(member -> leaderMap.put(member.getId(), member));

        return parties.stream()
                .map(party -> toPartySummaryResponse(party, leaderMap.get(party.getLeaderId())))
                .toList();
    }
}
