package com.skuri.skuri_backend.domain.minecraft.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.exception.MemberNotFoundException;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.minecraft.config.MinecraftBridgeProperties;
import com.skuri.skuri_backend.domain.minecraft.dto.response.MinecraftOverviewResponse;
import com.skuri.skuri_backend.domain.minecraft.dto.response.MinecraftPlayerResponse;
import com.skuri.skuri_backend.domain.minecraft.dto.response.MinecraftWhitelistEntryResponse;
import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftAccount;
import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftOnlinePlayer;
import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftServerState;
import com.skuri.skuri_backend.domain.minecraft.repository.MinecraftAccountRepository;
import com.skuri.skuri_backend.domain.minecraft.repository.MinecraftOnlinePlayerRepository;
import com.skuri.skuri_backend.domain.minecraft.repository.MinecraftServerStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MinecraftReadService {

    private final MinecraftServerStateRepository minecraftServerStateRepository;
    private final MinecraftOnlinePlayerRepository minecraftOnlinePlayerRepository;
    private final MinecraftAccountRepository minecraftAccountRepository;
    private final MemberRepository memberRepository;
    private final MinecraftBridgeProperties bridgeProperties;

    @Transactional(readOnly = true)
    public MinecraftOverviewResponse getOverview(String memberId) {
        requireActiveMember(memberId);
        MinecraftServerState state = minecraftServerStateRepository.findById(bridgeProperties.normalizedServerKey())
                .orElseThrow(() -> new BusinessException(ErrorCode.MINECRAFT_SERVER_UNAVAILABLE));
        return toOverviewResponse(state);
    }

    @Transactional(readOnly = true)
    public java.util.List<MinecraftPlayerResponse> getPlayers(String memberId) {
        requireActiveMember(memberId);
        return getPlayersFromSystem();
    }

    @Transactional(readOnly = true)
    public java.util.List<MinecraftPlayerResponse> getPlayersFromSystem() {
        java.util.List<MinecraftAccount> accounts = minecraftAccountRepository.findAllByOrderByCreatedAtAsc();
        java.util.Map<String, MinecraftAccount> accountMap = accounts.stream()
                .collect(Collectors.toMap(MinecraftAccount::getId, Function.identity()));
        java.util.Map<String, MinecraftOnlinePlayer> onlinePlayerMap = minecraftOnlinePlayerRepository
                .findByServerKeyOrderByPlayerNameAsc(bridgeProperties.normalizedServerKey())
                .stream()
                .collect(Collectors.toMap(MinecraftOnlinePlayer::getNormalizedKey, Function.identity(), (left, right) -> left));

        return accounts.stream()
                .map(account -> toPlayerResponse(account, accountMap, onlinePlayerMap))
                .toList();
    }

    @Transactional(readOnly = true)
    public java.util.List<MinecraftWhitelistEntryResponse> getWhitelistEntries() {
        return minecraftAccountRepository.findAllByOrderByCreatedAtAsc().stream()
                .map(account -> new MinecraftWhitelistEntryResponse(
                        account.getId(),
                        account.getNormalizedKey(),
                        account.getEdition(),
                        account.getGameName(),
                        account.getAvatarUuid(),
                        account.getStoredName()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public MinecraftOverviewResponse getOverviewOrNull() {
        return minecraftServerStateRepository.findById(bridgeProperties.normalizedServerKey())
                .map(this::toOverviewResponse)
                .orElse(new MinecraftOverviewResponse(
                        bridgeProperties.normalizedRoomId(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ));
    }

    private MinecraftOverviewResponse toOverviewResponse(MinecraftServerState state) {
        return new MinecraftOverviewResponse(
                bridgeProperties.normalizedRoomId(),
                state.isOnline(),
                state.getCurrentPlayers(),
                state.getMaxPlayers(),
                state.getVersion(),
                state.getServerAddress(),
                state.getMapUrl(),
                state.getLastHeartbeatAt()
        );
    }

    private MinecraftPlayerResponse toPlayerResponse(
            MinecraftAccount account,
            Map<String, MinecraftAccount> accountMap,
            Map<String, MinecraftOnlinePlayer> onlinePlayerMap
    ) {
        MinecraftAccount parent = account.getParentAccountId() == null ? null : accountMap.get(account.getParentAccountId());
        return new MinecraftPlayerResponse(
                account.getId(),
                account.getOwnerMemberId(),
                account.getAccountRole(),
                account.getEdition(),
                account.getGameName(),
                account.getNormalizedKey(),
                account.getAvatarUuid(),
                parent == null ? null : parent.getGameName(),
                onlinePlayerMap.containsKey(account.getNormalizedKey()),
                account.getLastSeenAt()
        );
    }

    private Member requireActiveMember(String memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);
        if (!member.isActive()) {
            throw new BusinessException(ErrorCode.MEMBER_WITHDRAWN);
        }
        return member;
    }
}
