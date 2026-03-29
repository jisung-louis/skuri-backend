package com.skuri.skuri_backend.domain.support.controller;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.support.dto.response.CafeteriaMenuReactionResponse;
import com.skuri.skuri_backend.domain.support.model.CafeteriaMenuReactionType;
import com.skuri.skuri_backend.domain.support.service.CafeteriaMenuReactionService;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CafeteriaMenuReactionController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class CafeteriaMenuReactionControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CafeteriaMenuReactionService cafeteriaMenuReactionService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @Test
    void upsertReaction_정상요청_200() throws Exception {
        mockUserToken("user-token");
        when(cafeteriaMenuReactionService.upsertReaction(
                eq("user-uid"),
                eq("2026-W08.rollNoodles.c4973864db4f8815"),
                eq(new com.skuri.skuri_backend.domain.support.dto.request.UpsertCafeteriaMenuReactionRequest(CafeteriaMenuReactionType.LIKE))
        )).thenReturn(new CafeteriaMenuReactionResponse(
                "2026-W08.rollNoodles.c4973864db4f8815",
                CafeteriaMenuReactionType.LIKE,
                13,
                2
        ));

        mockMvc.perform(
                        put("/v1/cafeteria-menu-reactions/2026-W08.rollNoodles.c4973864db4f8815")
                                .header(AUTHORIZATION, "Bearer user-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "reaction": "LIKE"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.menuId").value("2026-W08.rollNoodles.c4973864db4f8815"))
                .andExpect(jsonPath("$.data.myReaction").value("LIKE"))
                .andExpect(jsonPath("$.data.likeCount").value(13))
                .andExpect(jsonPath("$.data.dislikeCount").value(2));
    }

    @Test
    void upsertReaction_취소요청_200() throws Exception {
        mockUserToken("user-token");
        when(cafeteriaMenuReactionService.upsertReaction(
                eq("user-uid"),
                eq("2026-W08.rollNoodles.c4973864db4f8815"),
                eq(new com.skuri.skuri_backend.domain.support.dto.request.UpsertCafeteriaMenuReactionRequest(null))
        )).thenReturn(new CafeteriaMenuReactionResponse(
                "2026-W08.rollNoodles.c4973864db4f8815",
                null,
                12,
                2
        ));

        mockMvc.perform(
                        put("/v1/cafeteria-menu-reactions/2026-W08.rollNoodles.c4973864db4f8815")
                                .header(AUTHORIZATION, "Bearer user-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "reaction": null
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.myReaction").isEmpty())
                .andExpect(jsonPath("$.data.likeCount").value(12))
                .andExpect(jsonPath("$.data.dislikeCount").value(2));
    }

    @Test
    void upsertReaction_토큰없음_401() throws Exception {
        mockMvc.perform(
                        put("/v1/cafeteria-menu-reactions/2026-W08.rollNoodles.c4973864db4f8815")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "reaction": "LIKE"
                                        }
                                        """)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void upsertReaction_잘못된menuId_400() throws Exception {
        mockUserToken("user-token");
        when(cafeteriaMenuReactionService.upsertReaction(
                eq("user-uid"),
                eq("invalid"),
                eq(new com.skuri.skuri_backend.domain.support.dto.request.UpsertCafeteriaMenuReactionRequest(CafeteriaMenuReactionType.LIKE))
        )).thenThrow(new BusinessException(ErrorCode.INVALID_REQUEST, "menuId 형식이 올바르지 않습니다."));

        mockMvc.perform(
                        put("/v1/cafeteria-menu-reactions/invalid")
                                .header(AUTHORIZATION, "Bearer user-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "reaction": "LIKE"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
    }

    @Test
    void upsertReaction_메뉴항목없음_404() throws Exception {
        mockUserToken("user-token");
        when(cafeteriaMenuReactionService.upsertReaction(
                eq("user-uid"),
                eq("2026-W08.rollNoodles.c4973864db4f8815"),
                eq(new com.skuri.skuri_backend.domain.support.dto.request.UpsertCafeteriaMenuReactionRequest(CafeteriaMenuReactionType.LIKE))
        )).thenThrow(new BusinessException(ErrorCode.CAFETERIA_MENU_ENTRY_NOT_FOUND));

        mockMvc.perform(
                        put("/v1/cafeteria-menu-reactions/2026-W08.rollNoodles.c4973864db4f8815")
                                .header(AUTHORIZATION, "Bearer user-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "reaction": "LIKE"
                                        }
                                        """)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CAFETERIA_MENU_ENTRY_NOT_FOUND"));
    }

    private void mockUserToken(String token) {
        when(firebaseTokenVerifier.verify(token))
                .thenReturn(new FirebaseTokenClaims(
                        "user-uid",
                        "user@sungkyul.ac.kr",
                        "google.com",
                        "provider-id",
                        "홍길동",
                        "https://example.com/profile.jpg"
                ));
    }
}
