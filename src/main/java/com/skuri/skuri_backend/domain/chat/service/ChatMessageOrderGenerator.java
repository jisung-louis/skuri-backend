package com.skuri.skuri_backend.domain.chat.service;

import org.springframework.stereotype.Component;

@Component
public class ChatMessageOrderGenerator {

    private long lastIssuedOrder = 0L;

    public synchronized long nextOrder() {
        long candidate = System.currentTimeMillis() * 1_000L;
        if (candidate <= lastIssuedOrder) {
            candidate = lastIssuedOrder + 1L;
        }
        lastIssuedOrder = candidate;
        return candidate;
    }
}
