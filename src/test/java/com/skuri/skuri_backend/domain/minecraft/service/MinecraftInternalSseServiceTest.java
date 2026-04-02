package com.skuri.skuri_backend.domain.minecraft.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skuri.skuri_backend.domain.minecraft.repository.MinecraftBridgeEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MinecraftInternalSseServiceTest {

    @Mock
    private MinecraftReadService minecraftReadService;

    @Mock
    private MinecraftBridgeEventRepository minecraftBridgeEventRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void deserializePayload_저장된Json문자열이면_plainMap으로변환한다() throws IOException {
        MinecraftInternalSseService service = new MinecraftInternalSseService(
                minecraftReadService,
                minecraftBridgeEventRepository,
                objectMapper
        );

        Object payload = service.deserializePayload("""
                {
                  "messageId": "message-1",
                  "chatRoomId": "public:game:minecraft",
                  "senderName": "홍길동",
                  "type": "TEXT",
                  "text": "안녕하세요"
                }
                """);

        assertThat(payload).isInstanceOf(Map.class);
        assertThat(payload).isEqualTo(Map.of(
                "messageId", "message-1",
                "chatRoomId", "public:game:minecraft",
                "senderName", "홍길동",
                "type", "TEXT",
                "text", "안녕하세요"
        ));
    }

    @Test
    void deserializePayload_배열Json문자열이면_plainList로변환한다() throws IOException {
        MinecraftInternalSseService service = new MinecraftInternalSseService(
                minecraftReadService,
                minecraftBridgeEventRepository,
                objectMapper
        );

        Object payload = service.deserializePayload("""
                ["alpha", "beta"]
                """);

        assertThat(payload).isInstanceOf(List.class);
        assertThat(payload).isEqualTo(List.of("alpha", "beta"));
    }
}
