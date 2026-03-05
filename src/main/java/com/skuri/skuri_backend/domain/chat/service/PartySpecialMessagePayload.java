package com.skuri.skuri_backend.domain.chat.service;

import com.skuri.skuri_backend.domain.chat.entity.ChatAccountData;
import com.skuri.skuri_backend.domain.chat.entity.ChatArrivalData;

public record PartySpecialMessagePayload(
        String text,
        ChatAccountData accountData,
        ChatArrivalData arrivalData
) {
}
