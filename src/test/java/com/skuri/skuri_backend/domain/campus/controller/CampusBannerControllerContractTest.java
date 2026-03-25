package com.skuri.skuri_backend.domain.campus.controller;

import com.skuri.skuri_backend.domain.campus.dto.response.CampusBannerPublicResponse;
import com.skuri.skuri_backend.domain.campus.entity.CampusBannerActionTarget;
import com.skuri.skuri_backend.domain.campus.entity.CampusBannerActionType;
import com.skuri.skuri_backend.domain.campus.entity.CampusBannerPaletteKey;
import com.skuri.skuri_backend.domain.campus.service.CampusBannerService;
import com.skuri.skuri_backend.infra.auth.config.ApiAccessDeniedHandler;
import com.skuri.skuri_backend.infra.auth.config.ApiAuthenticationEntryPoint;
import com.skuri.skuri_backend.infra.auth.config.SecurityConfig;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseAuthenticationFilter;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseTokenVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CampusBannerController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class CampusBannerControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CampusBannerService campusBannerService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @Test
    void getCampusBanners_비인증정상요청_200() throws Exception {
        when(campusBannerService.getPublicBanners()).thenReturn(List.of(
                new CampusBannerPublicResponse(
                        "banner-1",
                        "택시 파티",
                        "택시 동승 매칭",
                        "같은 방향 가는 학생과 택시비를 함께 나눠요",
                        "파티 찾기",
                        CampusBannerPaletteKey.GREEN,
                        "https://cdn.skuri.app/uploads/campus-banners/2026/03/25/banner-1.jpg",
                        CampusBannerActionType.IN_APP,
                        CampusBannerActionTarget.TAXI_MAIN,
                        Map.of("initialView", "all"),
                        null
                )
        ));

        mockMvc.perform(get("/v1/campus-banners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("banner-1"))
                .andExpect(jsonPath("$.data[0].actionParams.initialView").value("all"));
    }
}
