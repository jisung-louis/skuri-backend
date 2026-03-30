package com.skuri.skuri_backend.domain.minecraft.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.minecraft.config.MinecraftBridgeProperties;
import com.skuri.skuri_backend.domain.minecraft.dto.request.CreateMinecraftAccountRequest;
import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftAccountRole;
import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftEdition;
import com.skuri.skuri_backend.domain.minecraft.repository.MinecraftAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@DataJpaTest
@Import({
        MinecraftAccountService.class,
        MinecraftIdentityService.class,
        MinecraftAccountServiceDataJpaTest.MinecraftBridgeTestConfig.class
})
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class MinecraftAccountServiceDataJpaTest {

    @Autowired
    private MinecraftAccountService minecraftAccountService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MinecraftAccountRepository minecraftAccountRepository;

    @MockitoBean
    private MojangProfileLookupClient mojangProfileLookupClient;

    @MockitoBean
    private MinecraftBridgeOutboxService minecraftBridgeOutboxService;

    @BeforeEach
    void setUp() {
        memberRepository.save(Member.create(
                "member-1",
                "user@sungkyul.ac.kr",
                "홍길동",
                LocalDateTime.now()
        ));
        when(mojangProfileLookupClient.lookup("skuriPlayer"))
                .thenReturn(new MojangProfileLookupClient.MojangProfile(
                        "skuriPlayer",
                        "8667ba71b85a4004af54457a9734eed7"
                ));
        when(mojangProfileLookupClient.lookup("friendPlayer"))
                .thenReturn(new MojangProfileLookupClient.MojangProfile(
                        "friendPlayer",
                        "11111111111111111111111111111111"
                ));
    }

    @Test
    void createAccount_본인계정등록_성공() {
        minecraftAccountService.createAccount("member-1", new CreateMinecraftAccountRequest(
                MinecraftEdition.JAVA,
                MinecraftAccountRole.SELF,
                "skuriPlayer"
        ));

        assertThat(minecraftAccountRepository.findByOwnerMemberIdOrderByCreatedAtAsc("member-1")).hasSize(1);
    }

    @Test
    void createAccount_본인계정중복_실패() {
        minecraftAccountService.createAccount("member-1", new CreateMinecraftAccountRequest(
                MinecraftEdition.JAVA,
                MinecraftAccountRole.SELF,
                "skuriPlayer"
        ));

        assertThatThrownBy(() -> minecraftAccountService.createAccount("member-1", new CreateMinecraftAccountRequest(
                MinecraftEdition.JAVA,
                MinecraftAccountRole.SELF,
                "friendPlayer"
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MINECRAFT_SELF_ACCOUNT_ALREADY_EXISTS);
    }

    @Test
    void createAccount_친구계정만먼저등록_실패() {
        assertThatThrownBy(() -> minecraftAccountService.createAccount("member-1", new CreateMinecraftAccountRequest(
                MinecraftEdition.JAVA,
                MinecraftAccountRole.FRIEND,
                "friendPlayer"
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MINECRAFT_PARENT_ACCOUNT_REQUIRED);
    }

    @Test
    void deleteAccount_친구연결된본인계정삭제_실패() {
        minecraftAccountService.createAccount("member-1", new CreateMinecraftAccountRequest(
                MinecraftEdition.JAVA,
                MinecraftAccountRole.SELF,
                "skuriPlayer"
        ));
        minecraftAccountService.createAccount("member-1", new CreateMinecraftAccountRequest(
                MinecraftEdition.JAVA,
                MinecraftAccountRole.FRIEND,
                "friendPlayer"
        ));

        String selfAccountId = minecraftAccountRepository.findByOwnerMemberIdOrderByCreatedAtAsc("member-1").stream()
                .filter(account -> account.getAccountRole() == MinecraftAccountRole.SELF)
                .findFirst()
                .orElseThrow()
                .getId();

        assertThatThrownBy(() -> minecraftAccountService.deleteAccount("member-1", selfAccountId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MINECRAFT_PARENT_ACCOUNT_DELETE_NOT_ALLOWED);
    }

    @TestConfiguration
    static class MinecraftBridgeTestConfig {

        @Bean
        MinecraftBridgeProperties minecraftBridgeProperties() {
            return new MinecraftBridgeProperties(
                    "test-secret",
                    "public:game:minecraft",
                    "skuri",
                    "8667ba71b85a4004af54457a9734eed7",
                    3600L
            );
        }
    }
}
