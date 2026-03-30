package com.skuri.skuri_backend.domain.minecraft.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.exception.MemberNotFoundException;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.minecraft.dto.request.CreateMinecraftAccountRequest;
import com.skuri.skuri_backend.domain.minecraft.dto.response.MinecraftAccountResponse;
import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftAccount;
import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftAccountRole;
import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftEdition;
import com.skuri.skuri_backend.domain.minecraft.repository.MinecraftAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MinecraftAccountService {

    private static final int MAX_TOTAL_ACCOUNTS = 4;
    private static final int MAX_FRIEND_ACCOUNTS = 3;

    private final MinecraftAccountRepository minecraftAccountRepository;
    private final MemberRepository memberRepository;
    private final MojangProfileLookupClient mojangProfileLookupClient;
    private final MinecraftIdentityService minecraftIdentityService;
    private final MinecraftBridgeOutboxService minecraftBridgeOutboxService;

    @Transactional(readOnly = true)
    public List<MinecraftAccountResponse> getMyAccounts(String memberId) {
        requireActiveMember(memberId);
        return toResponses(minecraftAccountRepository.findByOwnerMemberIdOrderByCreatedAtAsc(memberId));
    }

    @Transactional
    public MinecraftAccountResponse createAccount(String memberId, CreateMinecraftAccountRequest request) {
        requireActiveMember(memberId);

        List<MinecraftAccount> currentAccounts = minecraftAccountRepository.findByOwnerMemberIdOrderByCreatedAtAsc(memberId);
        MinecraftAccount selfAccount = currentAccounts.stream()
                .filter(account -> account.getAccountRole() == MinecraftAccountRole.SELF)
                .findFirst()
                .orElse(null);
        long friendCount = currentAccounts.stream()
                .filter(account -> account.getAccountRole() == MinecraftAccountRole.FRIEND)
                .count();

        if (currentAccounts.size() >= MAX_TOTAL_ACCOUNTS) {
            throw new BusinessException(ErrorCode.MINECRAFT_ACCOUNT_LIMIT_EXCEEDED);
        }

        if (request.accountRole() == MinecraftAccountRole.SELF && selfAccount != null) {
            throw new BusinessException(ErrorCode.MINECRAFT_SELF_ACCOUNT_ALREADY_EXISTS);
        }

        if (request.accountRole() == MinecraftAccountRole.FRIEND && selfAccount == null) {
            throw new BusinessException(ErrorCode.MINECRAFT_PARENT_ACCOUNT_REQUIRED);
        }

        if (request.accountRole() == MinecraftAccountRole.FRIEND && friendCount >= MAX_FRIEND_ACCOUNTS) {
            throw new BusinessException(ErrorCode.MINECRAFT_FRIEND_ACCOUNT_LIMIT_EXCEEDED);
        }

        ResolvedMinecraftAccount resolved = resolveAccount(request.edition(), request.gameName());
        if (minecraftAccountRepository.existsByNormalizedKey(resolved.normalizedKey())) {
            throw new BusinessException(ErrorCode.MINECRAFT_ACCOUNT_DUPLICATED);
        }

        MinecraftAccount saved = minecraftAccountRepository.save(MinecraftAccount.create(
                memberId,
                request.accountRole() == MinecraftAccountRole.FRIEND ? selfAccount.getId() : null,
                request.accountRole(),
                request.edition(),
                resolved.gameName(),
                resolved.storedName(),
                resolved.normalizedKey(),
                resolved.avatarUuid()
        ));

        minecraftBridgeOutboxService.publishWhitelistUpsert(saved);
        return toResponse(saved, selfAccount == null ? Map.of() : Map.of(selfAccount.getId(), selfAccount));
    }

    @Transactional
    public MinecraftAccountResponse deleteAccount(String memberId, String accountId) {
        requireActiveMember(memberId);

        MinecraftAccount target = minecraftAccountRepository.findByIdAndOwnerMemberId(accountId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "등록된 마인크래프트 계정을 찾을 수 없습니다."));

        if (target.getAccountRole() == MinecraftAccountRole.SELF
                && !minecraftAccountRepository.findByParentAccountId(target.getId()).isEmpty()) {
            throw new BusinessException(ErrorCode.MINECRAFT_PARENT_ACCOUNT_DELETE_NOT_ALLOWED);
        }

        Map<String, MinecraftAccount> allAccounts = minecraftAccountRepository.findByOwnerMemberIdOrderByCreatedAtAsc(memberId).stream()
                .collect(Collectors.toMap(MinecraftAccount::getId, Function.identity()));
        minecraftAccountRepository.delete(target);
        minecraftBridgeOutboxService.publishWhitelistRemove(target);
        return toResponse(target, allAccounts);
    }

    private ResolvedMinecraftAccount resolveAccount(MinecraftEdition edition, String gameName) {
        if (edition == MinecraftEdition.JAVA) {
            MojangProfileLookupClient.MojangProfile mojangProfile = mojangProfileLookupClient.lookup(gameName);
            return new ResolvedMinecraftAccount(
                    mojangProfile.resolvedName(),
                    null,
                    mojangProfile.normalizedUuid(),
                    minecraftIdentityService.resolveAvatarUuid(edition, mojangProfile.normalizedUuid())
            );
        }

        String normalizedGameName = minecraftIdentityService.normalizeGameName(gameName);
        String normalizedKey = minecraftIdentityService.normalizeAccountKey(edition, normalizedGameName, null);
        return new ResolvedMinecraftAccount(
                normalizedGameName,
                minecraftIdentityService.resolveStoredName(edition, normalizedGameName, normalizedKey),
                normalizedKey,
                minecraftIdentityService.resolveAvatarUuid(edition, normalizedKey)
        );
    }

    private List<MinecraftAccountResponse> toResponses(List<MinecraftAccount> accounts) {
        Map<String, MinecraftAccount> accountMap = accounts.stream()
                .collect(Collectors.toMap(MinecraftAccount::getId, Function.identity()));
        return accounts.stream()
                .map(account -> toResponse(account, accountMap))
                .toList();
    }

    private MinecraftAccountResponse toResponse(MinecraftAccount account, Map<String, MinecraftAccount> accountMap) {
        MinecraftAccount parent = account.getParentAccountId() == null ? null : accountMap.get(account.getParentAccountId());
        return new MinecraftAccountResponse(
                account.getId(),
                account.getAccountRole(),
                account.getEdition(),
                account.getGameName(),
                account.getNormalizedKey(),
                account.getAvatarUuid(),
                account.getStoredName(),
                account.getParentAccountId(),
                parent == null ? null : parent.getGameName(),
                account.getLastSeenAt(),
                account.getCreatedAt() == null ? null : account.getCreatedAt().atZone(java.time.ZoneId.of("Asia/Seoul")).toInstant()
        );
    }

    private Member requireActiveMember(String memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);
        if (!member.isActive()) {
            throw new BusinessException(ErrorCode.MEMBER_WITHDRAWN);
        }
        return member;
    }

    private record ResolvedMinecraftAccount(
            String gameName,
            String storedName,
            String normalizedKey,
            String avatarUuid
    ) {
    }
}
