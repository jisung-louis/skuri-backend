package com.skuri.skuri_backend.domain.minecraft.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.minecraft.dto.request.CreateMinecraftAccountRequest;
import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftAccount;
import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftAccountRole;
import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftEdition;
import com.skuri.skuri_backend.domain.minecraft.repository.MinecraftAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MinecraftAccountServiceTest {

    @Mock
    private MinecraftAccountRepository minecraftAccountRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MojangProfileLookupClient mojangProfileLookupClient;

    @Mock
    private MinecraftIdentityService minecraftIdentityService;

    @Mock
    private MinecraftBridgeOutboxService minecraftBridgeOutboxService;

    @InjectMocks
    private MinecraftAccountService minecraftAccountService;

    @BeforeEach
    void setUp() {
        when(memberRepository.findById("member-1"))
                .thenReturn(Optional.of(Member.create(
                        "member-1",
                        "user@sungkyul.ac.kr",
                        "홍길동",
                        LocalDateTime.now()
                )));
        when(minecraftAccountRepository.findByOwnerMemberIdOrderByCreatedAtAsc("member-1"))
                .thenReturn(List.of());
        when(mojangProfileLookupClient.lookup("skuriPlayer"))
                .thenReturn(new MojangProfileLookupClient.MojangProfile(
                        "skuriPlayer",
                        "8667ba71b85a4004af54457a9734eed7"
                ));
        when(minecraftIdentityService.resolveAvatarUuid(
                MinecraftEdition.JAVA,
                "8667ba71b85a4004af54457a9734eed7"
        )).thenReturn("8667ba71b85a4004af54457a9734eed7");
        when(minecraftAccountRepository.existsByNormalizedKey("8667ba71b85a4004af54457a9734eed7"))
                .thenReturn(false);
    }

    @Test
    void createAccount_저장경합으로_unique충돌나면_409로변환한다() {
        when(minecraftAccountRepository.saveAndFlush(any(MinecraftAccount.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate normalized_key"));

        assertThatThrownBy(() -> minecraftAccountService.createAccount(
                "member-1",
                new CreateMinecraftAccountRequest(MinecraftEdition.JAVA, MinecraftAccountRole.SELF, "skuriPlayer")
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MINECRAFT_ACCOUNT_DUPLICATED);
    }
}
