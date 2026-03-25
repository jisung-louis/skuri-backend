package com.skuri.skuri_backend.domain.chat.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ChatArrivalSettlementMemberData {

    private String memberId;
    private String displayName;
    private boolean settled;
    private LocalDateTime settledAt;
    private boolean leftParty;
    private LocalDateTime leftAt;
}
