package com.skuri.skuri_backend.domain.minecraft.service;

import com.skuri.skuri_backend.domain.chat.entity.ChatMessageDirection;
import com.skuri.skuri_backend.domain.chat.entity.ChatMessageType;
import com.skuri.skuri_backend.domain.chat.service.ChatService;
import com.skuri.skuri_backend.domain.minecraft.config.MinecraftBridgeProperties;
import com.skuri.skuri_backend.domain.minecraft.dto.request.MinecraftInternalChatMessageRequest;
import com.skuri.skuri_backend.domain.minecraft.dto.request.MinecraftOnlinePlayersUpsertRequest;
import com.skuri.skuri_backend.domain.minecraft.dto.request.MinecraftServerStateUpsertRequest;
import com.skuri.skuri_backend.domain.minecraft.dto.response.MinecraftPlayerResponse;
import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftAccount;
import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftOnlinePlayer;
import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftServerState;
import com.skuri.skuri_backend.domain.minecraft.repository.MinecraftAccountRepository;
import com.skuri.skuri_backend.domain.minecraft.repository.MinecraftInboundEventRepository;
import com.skuri.skuri_backend.domain.minecraft.repository.MinecraftOnlinePlayerRepository;
import com.skuri.skuri_backend.domain.minecraft.repository.MinecraftServerStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MinecraftBridgeService {

    private final MinecraftBridgeProperties bridgeProperties;
    private final MinecraftIdentityService minecraftIdentityService;
    private final MinecraftAvatarService minecraftAvatarService;
    private final MinecraftServerStateRepository minecraftServerStateRepository;
    private final MinecraftOnlinePlayerRepository minecraftOnlinePlayerRepository;
    private final MinecraftAccountRepository minecraftAccountRepository;
    private final MinecraftInboundEventRepository minecraftInboundEventRepository;
    private final MinecraftPublicSseService minecraftPublicSseService;
    private final MinecraftReadService minecraftReadService;
    private final ChatService chatService;

    @Transactional
    public void handleIncomingChatMessage(MinecraftInternalChatMessageRequest request) {
        if (minecraftInboundEventRepository.claimEvent(request.eventId()) == 0) {
            return;
        }

        String normalizedKey = minecraftIdentityService.normalizeAccountKey(
                request.edition(),
                request.senderName(),
                request.minecraftUuid()
        );
        String senderPhotoUrl = minecraftAvatarService.resolveAvatarUrl(
                minecraftIdentityService.toDisplayUuid(normalizedKey)
        );
        ChatMessageType messageType = request.eventType() == com.skuri.skuri_backend.domain.minecraft.entity.MinecraftInboundEventType.CHAT
                ? ChatMessageType.TEXT
                : ChatMessageType.SYSTEM;
        ChatMessageDirection direction = request.eventType() == com.skuri.skuri_backend.domain.minecraft.entity.MinecraftInboundEventType.CHAT
                ? ChatMessageDirection.MC_TO_APP
                : ChatMessageDirection.SYSTEM;

        String messageId = chatService.createMinecraftInboundMessage(
                minecraftIdentityService.toSyntheticSenderId(normalizedKey, request.senderName(), request.edition()),
                request.senderName(),
                senderPhotoUrl,
                request.text(),
                messageType,
                direction,
                normalizedKey,
                request.eventId()
        ).id();
        minecraftInboundEventRepository.markProcessed(request.eventId(), messageId);
    }

    @Transactional
    public void upsertServerState(MinecraftServerStateUpsertRequest request) {
        MinecraftServerState state = minecraftServerStateRepository.findById(bridgeProperties.normalizedServerKey())
                .orElseGet(() -> minecraftServerStateRepository.save(MinecraftServerState.create(bridgeProperties.normalizedServerKey())));
        state.update(
                request.online(),
                request.currentPlayers(),
                request.maxPlayers(),
                request.version(),
                request.serverAddress(),
                request.mapUrl(),
                request.heartbeatAt()
        );
        minecraftServerStateRepository.save(state);
        minecraftPublicSseService.publishServerStateUpdated(minecraftReadService.getOverviewOrNull());
    }

    @Transactional
    public void replaceOnlinePlayers(MinecraftOnlinePlayersUpsertRequest request) {
        String serverKey = bridgeProperties.normalizedServerKey();
        List<MinecraftOnlinePlayer> existingPlayers = minecraftOnlinePlayerRepository.findByServerKeyOrderByPlayerNameAsc(serverKey);
        Map<String, MinecraftOnlinePlayer> existingByKey = existingPlayers.stream()
                .collect(Collectors.toMap(MinecraftOnlinePlayer::getNormalizedKey, Function.identity(), (left, right) -> left));
        Map<String, MinecraftAccount> accountsByKey = minecraftAccountRepository.findAllByOrderByCreatedAtAsc().stream()
                .collect(Collectors.toMap(MinecraftAccount::getNormalizedKey, Function.identity(), (left, right) -> left));

        Map<String, MinecraftOnlinePlayersUpsertRequest.Player> nextByKey = new LinkedHashMap<>();
        for (MinecraftOnlinePlayersUpsertRequest.Player player : request.players()) {
            String normalizedKey = minecraftIdentityService.normalizeAccountKey(
                    player.edition(),
                    player.gameName(),
                    player.minecraftUuid()
            );
            nextByKey.put(normalizedKey, player);
        }

        minecraftOnlinePlayerRepository.deleteByServerKey(serverKey);

        for (Map.Entry<String, MinecraftOnlinePlayersUpsertRequest.Player> entry : nextByKey.entrySet()) {
            MinecraftOnlinePlayersUpsertRequest.Player player = entry.getValue();
            String normalizedKey = entry.getKey();
            MinecraftAccount account = accountsByKey.get(normalizedKey);
            if (account != null) {
                account.updateLastSeenAt(request.capturedAt());
            }
            minecraftOnlinePlayerRepository.save(MinecraftOnlinePlayer.create(
                    serverKey,
                    normalizedKey,
                    player.edition(),
                    player.gameName(),
                    minecraftIdentityService.resolveAvatarUuid(player.edition(), normalizedKey),
                    account == null ? null : account.getId(),
                    account != null,
                    request.capturedAt()
            ));
        }

        for (String removedKey : existingByKey.keySet()) {
            if (!nextByKey.containsKey(removedKey)) {
                minecraftPublicSseService.publishPlayerRemove(removedKey);
            }
        }

        List<MinecraftPlayerResponse> players = minecraftReadService.getPlayersFromSystem();
        minecraftPublicSseService.publishPlayersSnapshot(players);
    }
}
