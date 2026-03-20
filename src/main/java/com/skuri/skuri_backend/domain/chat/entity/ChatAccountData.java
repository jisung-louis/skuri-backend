package com.skuri.skuri_backend.domain.chat.entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ChatAccountData {

    private String bankName;
    private String accountNumber;
    private String accountHolder;
    private Boolean hideName;
}
