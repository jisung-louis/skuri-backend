package com.skuri.skuri_backend.domain.minecraft.controller;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.minecraft.config.MinecraftInternalSecretFilter;
import com.skuri.skuri_backend.domain.minecraft.dto.request.CreateMinecraftAccountRequest;
import com.skuri.skuri_backend.domain.minecraft.dto.response.MinecraftAccountResponse;
import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftAccountRole;
import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftEdition;
import com.skuri.skuri_backend.domain.minecraft.service.MinecraftAccountService;
import com.skuri.skuri_backend.infra.auth.config.ApiAccessDeniedHandler;
import com.skuri.skuri_backend.infra.auth.config.ApiAuthenticationEntryPoint;
import com.skuri.skuri_backend.infra.auth.config.SecurityConfig;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseAuthenticationFilter;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseTokenClaims;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseTokenVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MemberMinecraftAccountController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class,
        MinecraftInternalSecretFilter.class,
        MinecraftControllerTestConfig.class
})
class MemberMinecraftAccountControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MinecraftAccountService minecraftAccountService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @Test
    void getMyAccounts_정상조회_200() throws Exception {
        mockValidToken();
        when(minecraftAccountService.getMyAccounts("firebase-uid")).thenReturn(List.of(accountResponse()));

        mockMvc.perform(get("/v1/members/me/minecraft-accounts").header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].gameName").value("skuriPlayer"));
    }

    @Test
    void createAccount_정상생성_201() throws Exception {
        mockValidToken();
        when(minecraftAccountService.createAccount(org.mockito.ArgumentMatchers.eq("firebase-uid"), any(CreateMinecraftAccountRequest.class)))
                .thenReturn(accountResponse());

        mockMvc.perform(post("/v1/members/me/minecraft-accounts")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "edition": "JAVA",
                                  "accountRole": "SELF",
                                  "gameName": "skuriPlayer"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.accountRole").value("SELF"))
                .andExpect(jsonPath("$.data.normalizedKey").value("8667ba71b85a4004af54457a9734eed7"));
    }

    @Test
    void createAccount_검증실패_422() throws Exception {
        mockValidToken();

        mockMvc.perform(post("/v1/members/me/minecraft-accounts")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "edition": "JAVA",
                                  "accountRole": "SELF"
                                }
                                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verifyNoInteractions(minecraftAccountService);
    }

    @Test
    void createAccount_중복등록_409() throws Exception {
        mockValidToken();
        when(minecraftAccountService.createAccount(org.mockito.ArgumentMatchers.eq("firebase-uid"), any(CreateMinecraftAccountRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.MINECRAFT_ACCOUNT_DUPLICATED));

        mockMvc.perform(post("/v1/members/me/minecraft-accounts")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "edition": "JAVA",
                                  "accountRole": "SELF",
                                  "gameName": "skuriPlayer"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("MINECRAFT_ACCOUNT_DUPLICATED"));
    }

    @Test
    void deleteAccount_정상삭제_200() throws Exception {
        mockValidToken();
        when(minecraftAccountService.deleteAccount("firebase-uid", "account-1")).thenReturn(accountResponse());

        mockMvc.perform(delete("/v1/members/me/minecraft-accounts/account-1")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("account-1"));
    }

    @Test
    void deleteAccount_부모삭제불가_409() throws Exception {
        mockValidToken();
        when(minecraftAccountService.deleteAccount("firebase-uid", "account-1"))
                .thenThrow(new BusinessException(ErrorCode.MINECRAFT_PARENT_ACCOUNT_DELETE_NOT_ALLOWED));

        mockMvc.perform(delete("/v1/members/me/minecraft-accounts/account-1")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("MINECRAFT_PARENT_ACCOUNT_DELETE_NOT_ALLOWED"));
    }

    @Test
    void getMyAccounts_토큰없음_401() throws Exception {
        mockMvc.perform(get("/v1/members/me/minecraft-accounts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    private MinecraftAccountResponse accountResponse() {
        return new MinecraftAccountResponse(
                "account-1",
                MinecraftAccountRole.SELF,
                MinecraftEdition.JAVA,
                "skuriPlayer",
                "8667ba71b85a4004af54457a9734eed7",
                "8667ba71b85a4004af54457a9734eed7",
                null,
                null,
                null,
                Instant.parse("2026-03-30T13:18:00Z"),
                Instant.parse("2026-03-30T13:00:00Z")
        );
    }

    private void mockValidToken() {
        when(firebaseTokenVerifier.verify("valid-token"))
                .thenReturn(new FirebaseTokenClaims(
                        "firebase-uid",
                        "user@sungkyul.ac.kr",
                        "google.com",
                        "google-provider-id",
                        "홍길동",
                        "https://example.com/profile.jpg"
                ));
    }
}
