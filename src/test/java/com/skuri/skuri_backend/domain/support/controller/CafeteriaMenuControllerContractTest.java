package com.skuri.skuri_backend.domain.support.controller;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.support.dto.response.CafeteriaMenuResponse;
import com.skuri.skuri_backend.domain.support.service.CafeteriaMenuService;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CafeteriaMenuController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class CafeteriaMenuControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CafeteriaMenuService cafeteriaMenuService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @Test
    void getCurrentWeekMenu_정상요청_200() throws Exception {
        mockUserToken("user-token");
        when(cafeteriaMenuService.getCurrentWeekMenu(LocalDate.of(2026, 2, 3)))
                .thenReturn(menuResponse());

        mockMvc.perform(
                        get("/v1/cafeteria-menus")
                                .header(AUTHORIZATION, "Bearer user-token")
                                .param("date", "2026-02-03")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.weekId").value("2026-W06"));
    }

    @Test
    void getCurrentWeekMenu_토큰없음_401() throws Exception {
        mockMvc.perform(get("/v1/cafeteria-menus"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void getCurrentWeekMenu_메뉴없음_404() throws Exception {
        mockUserToken("user-token");
        when(cafeteriaMenuService.getCurrentWeekMenu(LocalDate.of(2026, 2, 3)))
                .thenThrow(new BusinessException(ErrorCode.CAFETERIA_MENU_NOT_FOUND));

        mockMvc.perform(
                        get("/v1/cafeteria-menus")
                                .header(AUTHORIZATION, "Bearer user-token")
                                .param("date", "2026-02-03")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CAFETERIA_MENU_NOT_FOUND"));
    }

    @Test
    void getMenuByWeekId_정상요청_200() throws Exception {
        mockUserToken("user-token");
        when(cafeteriaMenuService.getMenuByWeekId("2026-W06")).thenReturn(menuResponse());

        mockMvc.perform(get("/v1/cafeteria-menus/2026-W06").header(AUTHORIZATION, "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.weekId").value("2026-W06"));
    }

    @Test
    void getMenuByWeekId_잘못된weekId_400() throws Exception {
        mockUserToken("user-token");
        when(cafeteriaMenuService.getMenuByWeekId("invalid"))
                .thenThrow(new BusinessException(ErrorCode.INVALID_REQUEST, "weekId 형식은 yyyy-Www 이어야 합니다."));

        mockMvc.perform(get("/v1/cafeteria-menus/invalid").header(AUTHORIZATION, "Bearer user-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
    }

    private CafeteriaMenuResponse menuResponse() {
        return new CafeteriaMenuResponse(
                "2026-W06",
                LocalDate.of(2026, 2, 3),
                LocalDate.of(2026, 2, 7),
                Map.of(
                        "2026-02-03",
                        Map.of(
                                "rollNoodles", List.of("우동", "김밥"),
                                "theBab", List.of("돈까스", "된장찌개")
                        )
                )
        );
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
