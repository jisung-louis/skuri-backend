package com.skuri.skuri_backend.domain.chat.entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ChatArrivalData {

    private Integer taxiFare;
    private Integer perPersonAmount;
    private Integer splitMemberCount;
    private List<String> settlementTargetMemberIds;
    private ChatAccountData accountData;
}
