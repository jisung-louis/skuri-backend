package com.skuri.skuri_backend.domain.chat.entity;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ChatArrivalData {

    private Integer taxiFare;

    @JsonAlias("perPerson")
    private Integer perPersonAmount;

    @JsonAlias("memberCount")
    private Integer splitMemberCount;
    private List<String> settlementTargetMemberIds;
    private ChatAccountData accountData;
}
