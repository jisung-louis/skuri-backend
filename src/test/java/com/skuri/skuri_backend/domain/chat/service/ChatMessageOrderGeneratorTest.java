package com.skuri.skuri_backend.domain.chat.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatMessageOrderGeneratorTest {

    private final ChatMessageOrderGenerator generator = new ChatMessageOrderGenerator();

    @Test
    void nextOrder_연속호출시_단조증가한다() {
        long first = generator.nextOrder();
        long second = generator.nextOrder();

        assertTrue(second > first);
    }

    @Test
    void nextOrder_현재시각보다_이전값이더크면_이전값기준으로증가한다() {
        ReflectionTestUtils.setField(generator, "lastIssuedOrder", Long.MAX_VALUE - 1);

        long next = generator.nextOrder();

        assertEquals(Long.MAX_VALUE, next);
    }
}
