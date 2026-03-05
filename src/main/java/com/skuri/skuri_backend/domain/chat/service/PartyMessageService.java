package com.skuri.skuri_backend.domain.chat.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.chat.entity.ChatAccountData;
import com.skuri.skuri_backend.domain.chat.entity.ChatArrivalData;
import com.skuri.skuri_backend.domain.chat.entity.ChatMessageType;
import com.skuri.skuri_backend.domain.member.entity.BankAccount;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.exception.MemberNotFoundException;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.taxiparty.entity.Party;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus;
import com.skuri.skuri_backend.domain.taxiparty.exception.PartyNotFoundException;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PartyMessageService {

    private static final String PARTY_CHAT_ROOM_PREFIX = "party:";

    private final PartyRepository partyRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public PartySpecialMessagePayload buildSpecialPayload(
            String chatRoomId,
            String senderId,
            ChatMessageType type
    ) {
        String partyId = extractPartyId(chatRoomId);
        Party party = partyRepository.findDetailById(partyId).orElseThrow(PartyNotFoundException::new);
        if (!party.isMember(senderId)) {
            throw new BusinessException(ErrorCode.NOT_PARTY_MEMBER);
        }

        return switch (type) {
            case ACCOUNT -> buildAccountPayload(senderId);
            case ARRIVED -> buildArrivalPayload(party, senderId);
            case END -> buildEndPayload(party, senderId);
            default -> throw new BusinessException(ErrorCode.INVALID_REQUEST, "파티 특수 메시지 타입이 아닙니다.");
        };
    }

    private PartySpecialMessagePayload buildAccountPayload(String senderId) {
        Member member = memberRepository.findById(senderId).orElseThrow(MemberNotFoundException::new);
        BankAccount bankAccount = member.getBankAccount();
        if (bankAccount == null
                || !StringUtils.hasText(bankAccount.getBankName())
                || !StringUtils.hasText(bankAccount.getAccountNumber())
                || !StringUtils.hasText(bankAccount.getAccountHolder())) {
            throw new BusinessException(ErrorCode.BANK_ACCOUNT_REQUIRED);
        }

        ChatAccountData accountData = new ChatAccountData(
                bankAccount.getBankName(),
                bankAccount.getAccountNumber(),
                bankAccount.getAccountHolder()
        );
        String text = String.format("%s/%s", bankAccount.getBankName(), bankAccount.getAccountNumber());
        return new PartySpecialMessagePayload(text, accountData, null);
    }

    private PartySpecialMessagePayload buildArrivalPayload(Party party, String senderId) {
        if (!party.isLeader(senderId)) {
            throw new BusinessException(ErrorCode.NOT_PARTY_LEADER);
        }
        if (party.getStatus() != PartyStatus.ARRIVED) {
            throw new BusinessException(ErrorCode.INVALID_PARTY_STATE_TRANSITION, "ARRIVED 상태에서만 도착 메시지를 전송할 수 있습니다.");
        }

        Integer perPerson = party.getPerPersonAmount();
        int settlementTargetCount = party.getSettlementItems().size();
        Integer taxiFare = (perPerson != null && settlementTargetCount > 0)
                ? perPerson * settlementTargetCount
                : null;
        Integer memberCount = party.getCurrentMembers();
        ChatArrivalData arrivalData = new ChatArrivalData(
                taxiFare,
                perPerson,
                memberCount
        );

        String text = perPerson == null
                ? "파티가 도착했습니다."
                : "파티가 도착했습니다. 정산을 진행해주세요. (1인당 " + perPerson + "원)";
        return new PartySpecialMessagePayload(text, null, arrivalData);
    }

    private PartySpecialMessagePayload buildEndPayload(Party party, String senderId) {
        if (!party.isLeader(senderId)) {
            throw new BusinessException(ErrorCode.NOT_PARTY_LEADER);
        }
        if (party.getStatus() != PartyStatus.ENDED) {
            throw new BusinessException(ErrorCode.INVALID_PARTY_STATE_TRANSITION, "ENDED 상태에서만 종료 메시지를 전송할 수 있습니다.");
        }

        String text = party.getEndReason() == null
                ? "파티가 종료되었습니다."
                : "파티가 종료되었습니다. (" + party.getEndReason().name() + ")";
        return new PartySpecialMessagePayload(text, null, null);
    }

    private String extractPartyId(String chatRoomId) {
        if (!StringUtils.hasText(chatRoomId) || !chatRoomId.startsWith(PARTY_CHAT_ROOM_PREFIX)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "파티 채팅방 ID 형식이 아닙니다.");
        }
        return chatRoomId.substring(PARTY_CHAT_ROOM_PREFIX.length());
    }
}
