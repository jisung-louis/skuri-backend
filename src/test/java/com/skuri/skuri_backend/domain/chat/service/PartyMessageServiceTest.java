package com.skuri.skuri_backend.domain.chat.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.chat.entity.ChatMessageType;
import com.skuri.skuri_backend.domain.member.entity.BankAccount;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.taxiparty.entity.Location;
import com.skuri.skuri_backend.domain.taxiparty.entity.Party;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartyMessageServiceTest {

    @Mock
    private PartyRepository partyRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private PartyMessageService partyMessageService;

    @Test
    void buildSpecialPayload_ACCOUNT_계좌없으면_BANK_ACCOUNT_REQUIRED() {
        Party party = sampleParty("leader-1", true, false, false);
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(memberRepository.findById("leader-1"))
                .thenReturn(Optional.of(Member.create("leader-1", "leader-1@sungkyul.ac.kr", "리더", LocalDateTime.now())));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> partyMessageService.buildSpecialPayload("party:party-1", "leader-1", ChatMessageType.ACCOUNT)
        );

        assertEquals(ErrorCode.BANK_ACCOUNT_REQUIRED, exception.getErrorCode());
    }

    @Test
    void buildSpecialPayload_ARRIVED_성공() {
        Party party = sampleParty("leader-1", true, true, false);
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));

        PartySpecialMessagePayload payload = partyMessageService.buildSpecialPayload(
                "party:party-1",
                "leader-1",
                ChatMessageType.ARRIVED
        );

        assertEquals("파티가 도착했습니다. 정산을 진행해주세요. (1인당 14000원)", payload.text());
        assertEquals(14000, payload.arrivalData().getTaxiFare());
    }

    @Test
    void buildSpecialPayload_END_성공() {
        Party party = sampleParty("leader-1", true, true, true);
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));

        PartySpecialMessagePayload payload = partyMessageService.buildSpecialPayload(
                "party:party-1",
                "leader-1",
                ChatMessageType.END
        );

        assertEquals("파티가 종료되었습니다. (FORCE_ENDED)", payload.text());
    }

    private Party sampleParty(String leaderId, boolean includeMember, boolean arrived, boolean ended) {
        Party party = Party.create(
                leaderId,
                Location.of("성결대학교", 37.38, 126.93),
                Location.of("안양역", 37.40, 126.92),
                LocalDateTime.now().plusHours(2),
                4,
                List.of("빠른출발"),
                "테스트"
        );
        if (includeMember) {
            party.addMember("member-2");
        }
        if (arrived) {
            party.arrive(14000);
        }
        if (ended) {
            party.forceEnd();
        }
        ReflectionTestUtils.setField(party, "id", "party-1");
        return party;
    }
}
