package com.skuri.skuri_backend.domain.minecraft.service;

import com.skuri.skuri_backend.domain.chat.dto.response.ChatMessageResponse;
import com.skuri.skuri_backend.domain.chat.entity.ChatMessageDirection;
import com.skuri.skuri_backend.domain.chat.entity.ChatMessageType;
import com.skuri.skuri_backend.domain.chat.service.ChatService;
import com.skuri.skuri_backend.domain.minecraft.config.MinecraftBridgeProperties;
import com.skuri.skuri_backend.domain.minecraft.dto.request.MinecraftInternalChatMessageRequest;
import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftEdition;
import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftInboundEventType;
import com.skuri.skuri_backend.domain.minecraft.repository.MinecraftAccountRepository;
import com.skuri.skuri_backend.domain.minecraft.repository.MinecraftInboundEventRepository;
import com.skuri.skuri_backend.domain.minecraft.repository.MinecraftOnlinePlayerRepository;
import com.skuri.skuri_backend.domain.minecraft.repository.MinecraftServerStateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MinecraftBridgeServiceTest {

    @Mock
    private MinecraftBridgeProperties bridgeProperties;

    @Mock
    private MinecraftIdentityService minecraftIdentityService;

    @Mock
    private MinecraftAvatarService minecraftAvatarService;

    @Mock
    private MinecraftServerStateRepository minecraftServerStateRepository;

    @Mock
    private MinecraftOnlinePlayerRepository minecraftOnlinePlayerRepository;

    @Mock
    private MinecraftAccountRepository minecraftAccountRepository;

    @Mock
    private MinecraftInboundEventRepository minecraftInboundEventRepository;

    @Mock
    private MinecraftPublicSseService minecraftPublicSseService;

    @Mock
    private MinecraftReadService minecraftReadService;

    @Mock
    private ChatService chatService;

    @InjectMocks
    private MinecraftBridgeService minecraftBridgeService;

    @Test
    void handleIncomingChatMessage_중복EventId면_채팅을다시저장하지않는다() {
        MinecraftInternalChatMessageRequest request = new MinecraftInternalChatMessageRequest(
                "event-1",
                MinecraftInboundEventType.CHAT,
                null,
                "skuriPlayer",
                "8667ba71b85a4004af54457a9734eed7",
                MinecraftEdition.JAVA,
                "안녕하세요!",
                Instant.parse("2026-03-30T13:20:00Z")
        );
        when(minecraftInboundEventRepository.claimEvent("event-1")).thenReturn(0);

        minecraftBridgeService.handleIncomingChatMessage(request);

        verify(chatService, never()).createMinecraftInboundMessage(any(), any(), any(), any(), any(), any(), any(), any());
        verify(minecraftInboundEventRepository, never()).markProcessed(any(), any());
    }

    @Test
    void handleIncomingChatMessage_새이벤트면_저장후처리완료를기록한다() {
        MinecraftInternalChatMessageRequest request = new MinecraftInternalChatMessageRequest(
                "event-1",
                MinecraftInboundEventType.CHAT,
                null,
                "skuriPlayer",
                "8667ba71b85a4004af54457a9734eed7",
                MinecraftEdition.JAVA,
                "안녕하세요!",
                Instant.parse("2026-03-30T13:20:00Z")
        );
        when(minecraftInboundEventRepository.claimEvent("event-1")).thenReturn(1);
        when(minecraftIdentityService.normalizeAccountKey(
                MinecraftEdition.JAVA,
                "skuriPlayer",
                "8667ba71b85a4004af54457a9734eed7"
        )).thenReturn("8667ba71b85a4004af54457a9734eed7");
        when(minecraftIdentityService.toDisplayUuid("8667ba71b85a4004af54457a9734eed7"))
                .thenReturn("8667ba71b85a4004af54457a9734eed7");
        when(minecraftIdentityService.toSyntheticSenderId(
                "8667ba71b85a4004af54457a9734eed7",
                "skuriPlayer",
                MinecraftEdition.JAVA
        )).thenReturn("synthetic-sender");
        when(minecraftAvatarService.resolveAvatarUrl("8667ba71b85a4004af54457a9734eed7"))
                .thenReturn("https://minotar.net/avatar/8667ba71b85a4004af54457a9734eed7/64");
        when(chatService.createMinecraftInboundMessage(
                eq("synthetic-sender"),
                eq("skuriPlayer"),
                eq("https://minotar.net/avatar/8667ba71b85a4004af54457a9734eed7/64"),
                eq("안녕하세요!"),
                eq(ChatMessageType.TEXT),
                eq(ChatMessageDirection.MC_TO_APP),
                eq("8667ba71b85a4004af54457a9734eed7"),
                eq("event-1")
        )).thenReturn(new ChatMessageResponse(
                "message-1",
                "public:game:minecraft",
                "synthetic-sender",
                "skuriPlayer",
                null,
                ChatMessageType.TEXT,
                "안녕하세요!",
                null,
                null,
                null,
                null
        ));

        minecraftBridgeService.handleIncomingChatMessage(request);

        verify(minecraftInboundEventRepository).markProcessed("event-1", "message-1");
    }
}
