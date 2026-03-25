package com.skuri.skuri_backend.domain.chat.entity.converter;

import com.skuri.skuri_backend.domain.chat.entity.ChatArrivalData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ChatArrivalDataJsonConverterTest {

    private final ChatArrivalDataJsonConverter converter = new ChatArrivalDataJsonConverter();

    @Test
    void convertToEntityAttribute_이전ARRIVED필드명도역직렬화한다() {
        String legacyJson = """
                {
                  "taxiFare": 14000,
                  "perPerson": 7000,
                  "memberCount": 2,
                  "settlementTargetMemberIds": ["member-2"],
                  "accountData": {
                    "bankName": "카카오뱅크",
                    "accountNumber": "3333-03-1234567",
                    "accountHolder": "홍*동",
                    "hideName": true
                  }
                }
                """;

        ChatArrivalData arrivalData = converter.convertToEntityAttribute(legacyJson);

        assertNotNull(arrivalData);
        assertEquals(14000, arrivalData.getTaxiFare());
        assertEquals(7000, arrivalData.getPerPersonAmount());
        assertEquals(2, arrivalData.getSplitMemberCount());
        assertEquals(List.of("member-2"), arrivalData.getSettlementTargetMemberIds());
        assertNotNull(arrivalData.getAccountData());
        assertEquals("카카오뱅크", arrivalData.getAccountData().getBankName());
    }

    @Test
    void convertToEntityAttribute_정산대상상태필드도역직렬화한다() {
        String json = """
                {
                  "taxiFare": 14000,
                  "perPersonAmount": 7000,
                  "splitMemberCount": 2,
                  "settlementTargetMemberIds": ["member-2"],
                  "memberSettlements": [
                    {
                      "memberId": "member-2",
                      "displayName": "김철수",
                      "settled": false,
                      "settledAt": null,
                      "leftParty": true,
                      "leftAt": "2026-03-25T21:10:00"
                    }
                  ],
                  "accountData": {
                    "bankName": "카카오뱅크",
                    "accountNumber": "3333-03-1234567",
                    "accountHolder": "홍*동",
                    "hideName": true
                  }
                }
                """;

        ChatArrivalData arrivalData = converter.convertToEntityAttribute(json);

        assertNotNull(arrivalData);
        assertEquals(1, arrivalData.getMemberSettlements().size());
        assertEquals("김철수", arrivalData.getMemberSettlements().get(0).getDisplayName());
        assertEquals(true, arrivalData.getMemberSettlements().get(0).isLeftParty());
    }
}
