package com.skuri.skuri_backend.domain.chat.entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ChatArrivalData {

    private Integer taxiFare;
    private Integer perPerson;
    private Integer memberCount;
}
