package com.skuri.skuri_backend.domain.minecraft.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skuri.skuri_backend.domain.chat.entity.ChatMessage;
import com.skuri.skuri_backend.domain.chat.entity.ChatMessageType;
import com.skuri.skuri_backend.domain.minecraft.config.MinecraftBridgeProperties;
import com.skuri.skuri_backend.domain.minecraft.dto.response.MinecraftChatBridgeMessageResponse;
import com.skuri.skuri_backend.domain.minecraft.dto.response.MinecraftWhitelistEntryResponse;
import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftAccount;
import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftBridgeEvent;
import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftBridgeEventType;
import com.skuri.skuri_backend.domain.minecraft.repository.MinecraftBridgeEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class MinecraftBridgeOutboxService {

    private final MinecraftBridgeEventRepository minecraftBridgeEventRepository;
    private final MinecraftBridgeProperties bridgeProperties;
    private final ObjectMapper objectMapper;
    private final MinecraftInternalSseService minecraftInternalSseService;

    @Transactional
    public void publishChatFromApp(ChatMessage message) {
        if (message.getType() != ChatMessageType.TEXT && message.getType() != ChatMessageType.IMAGE) {
            return;
        }

        String text = message.getType() == ChatMessageType.IMAGE
                ? message.getSenderName() + "님이 사진을 보냈습니다."
                : message.getText();

        publish(MinecraftBridgeEventType.CHAT_FROM_APP, new MinecraftChatBridgeMessageResponse(
                message.getId(),
                message.getChatRoomId(),
                message.getSenderName(),
                message.getType(),
                text
        ));
    }

    @Transactional
    public void publishWhitelistUpsert(MinecraftAccount account) {
        publish(MinecraftBridgeEventType.WHITELIST_UPSERT, toWhitelistEntry(account));
    }

    @Transactional
    public void publishWhitelistRemove(MinecraftAccount account) {
        publish(MinecraftBridgeEventType.WHITELIST_REMOVE, toWhitelistEntry(account));
    }

    private void publish(MinecraftBridgeEventType eventType, Object payload) {
        MinecraftBridgeEvent event = minecraftBridgeEventRepository.save(MinecraftBridgeEvent.create(
                eventType,
                toJson(payload),
                Instant.now().plusSeconds(bridgeProperties.normalizedReplayTtlSeconds())
        ));
        minecraftInternalSseService.publishStoredEvent(event);
    }

    private MinecraftWhitelistEntryResponse toWhitelistEntry(MinecraftAccount account) {
        return new MinecraftWhitelistEntryResponse(
                account.getId(),
                account.getNormalizedKey(),
                account.getEdition(),
                account.getGameName(),
                account.getAvatarUuid(),
                account.getStoredName()
        );
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Minecraft bridge payload 직렬화에 실패했습니다.", e);
        }
    }
}
