package com.skuri.skuri_backend.domain.campus.controller;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.campus.dto.response.CampusBannerAdminResponse;
import com.skuri.skuri_backend.domain.campus.dto.response.CampusBannerOrderResponse;
import com.skuri.skuri_backend.domain.campus.entity.CampusBannerActionTarget;
import com.skuri.skuri_backend.domain.campus.entity.CampusBannerActionType;
import com.skuri.skuri_backend.domain.campus.entity.CampusBannerPaletteKey;
import com.skuri.skuri_backend.domain.campus.service.CampusBannerService;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CampusBannerAdminController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class CampusBannerAdminControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CampusBannerService campusBannerService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @MockitoBean
    private MemberRepository memberRepository;

    @Test
    void getCampusBanners_관리자정상요청_200() throws Exception {
        mockToken("admin-token", true);
        when(campusBannerService.getAdminBanners()).thenReturn(List.of(adminResponse("banner-1")));

        mockMvc.perform(get("/v1/admin/campus-banners").header(AUTHORIZATION, "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("banner-1"));
    }

    @Test
    void getCampusBanners_비관리자요청_403() throws Exception {
        mockToken("user-token", false);

        mockMvc.perform(get("/v1/admin/campus-banners").header(AUTHORIZATION, "Bearer user-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ADMIN_REQUIRED"));
    }

    @Test
    void getCampusBanner_관리자정상요청_200() throws Exception {
        mockToken("admin-token", true);
        when(campusBannerService.getAdminBanner("banner-1")).thenReturn(adminResponse("banner-1"));

        mockMvc.perform(get("/v1/admin/campus-banners/banner-1").header(AUTHORIZATION, "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("banner-1"));
    }

    @Test
    void getCampusBanner_없는배너_404() throws Exception {
        mockToken("admin-token", true);
        when(campusBannerService.getAdminBanner("missing"))
                .thenThrow(new BusinessException(ErrorCode.CAMPUS_BANNER_NOT_FOUND));

        mockMvc.perform(get("/v1/admin/campus-banners/missing").header(AUTHORIZATION, "Bearer admin-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CAMPUS_BANNER_NOT_FOUND"));
    }

    @Test
    void createCampusBanner_관리자정상요청_201() throws Exception {
        mockToken("admin-token", true);
        when(campusBannerService.createBanner(any())).thenReturn(adminResponse("banner-1"));

        mockMvc.perform(
                        post("/v1/admin/campus-banners")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content(validCreateRequest())
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value("banner-1"));
    }

    @Test
    void createCampusBanner_요청검증실패_422() throws Exception {
        mockToken("admin-token", true);

        mockMvc.perform(
                        post("/v1/admin/campus-banners")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "badgeLabel": "",
                                          "titleLabel": "",
                                          "descriptionLabel": "",
                                          "buttonLabel": "",
                                          "paletteKey": null,
                                          "imageUrl": "",
                                          "actionType": null,
                                          "isActive": null
                                        }
                                        """)
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void updateCampusBanner_관리자정상요청_200() throws Exception {
        mockToken("admin-token", true);
        when(campusBannerService.updateBanner(eq("banner-1"), any())).thenReturn(adminResponse("banner-1"));

        mockMvc.perform(
                        patch("/v1/admin/campus-banners/banner-1")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "buttonLabel": "공지 보기",
                                          "paletteKey": "BLUE",
                                          "actionType": "IN_APP",
                                          "actionTarget": "NOTICE_MAIN",
                                          "actionParams": null,
                                          "actionUrl": null,
                                          "isActive": true
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("banner-1"));
    }

    @Test
    void updateCampusBanner_없는배너_404() throws Exception {
        mockToken("admin-token", true);
        when(campusBannerService.updateBanner(eq("missing"), any()))
                .thenThrow(new BusinessException(ErrorCode.CAMPUS_BANNER_NOT_FOUND));

        mockMvc.perform(
                        patch("/v1/admin/campus-banners/missing")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "buttonLabel": "공지 보기"
                                        }
                                        """)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CAMPUS_BANNER_NOT_FOUND"));
    }

    @Test
    void deleteCampusBanner_관리자정상요청_200() throws Exception {
        mockToken("admin-token", true);

        mockMvc.perform(delete("/v1/admin/campus-banners/banner-1").header(AUTHORIZATION, "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void deleteCampusBanner_없는배너_404() throws Exception {
        mockToken("admin-token", true);
        doThrow(new BusinessException(ErrorCode.CAMPUS_BANNER_NOT_FOUND))
                .when(campusBannerService).deleteBanner("missing");

        mockMvc.perform(delete("/v1/admin/campus-banners/missing").header(AUTHORIZATION, "Bearer admin-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CAMPUS_BANNER_NOT_FOUND"));
    }

    @Test
    void reorderCampusBanners_관리자정상요청_200() throws Exception {
        mockToken("admin-token", true);
        when(campusBannerService.reorderBanners(any())).thenReturn(List.of(
                new CampusBannerOrderResponse("banner-2", 1),
                new CampusBannerOrderResponse("banner-1", 2)
        ));

        mockMvc.perform(
                        put("/v1/admin/campus-banners/order")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "bannerIds": ["banner-2", "banner-1"]
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("banner-2"))
                .andExpect(jsonPath("$.data[0].displayOrder").value(1));
    }

    @Test
    void reorderCampusBanners_요청검증실패_422() throws Exception {
        mockToken("admin-token", true);

        mockMvc.perform(
                        put("/v1/admin/campus-banners/order")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "bannerIds": []
                                        }
                                        """)
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void reorderCampusBanners_101개배너도_검증통과() throws Exception {
        mockToken("admin-token", true);
        when(campusBannerService.reorderBanners(any())).thenReturn(List.of());

        mockMvc.perform(
                        put("/v1/admin/campus-banners/order")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content(reorderRequest(101))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    private String validCreateRequest() {
        return """
                {
                  "badgeLabel": "택시 파티",
                  "titleLabel": "택시 동승 매칭",
                  "descriptionLabel": "같은 방향 가는 학생과 택시비를 함께 나눠요",
                  "buttonLabel": "파티 찾기",
                  "paletteKey": "GREEN",
                  "imageUrl": "https://cdn.skuri.app/uploads/campus-banners/2026/03/25/banner-1.jpg",
                  "actionType": "IN_APP",
                  "actionTarget": "TAXI_MAIN",
                  "actionParams": null,
                  "actionUrl": null,
                  "isActive": true,
                  "displayStartAt": "2026-03-25T00:00:00",
                  "displayEndAt": null
                }
                """;
    }

    private String reorderRequest(int count) {
        String bannerIds = IntStream.rangeClosed(1, count)
                .mapToObj(index -> "\"banner-" + index + "\"")
                .collect(Collectors.joining(", "));
        return """
                {
                  "bannerIds": [%s]
                }
                """.formatted(bannerIds);
    }

    private CampusBannerAdminResponse adminResponse(String id) {
        return new CampusBannerAdminResponse(
                id,
                "택시 파티",
                "택시 동승 매칭",
                "같은 방향 가는 학생과 택시비를 함께 나눠요",
                "파티 찾기",
                CampusBannerPaletteKey.GREEN,
                "https://cdn.skuri.app/uploads/campus-banners/2026/03/25/banner-1.jpg",
                CampusBannerActionType.IN_APP,
                CampusBannerActionTarget.TAXI_MAIN,
                Map.of("initialView", "all"),
                null,
                true,
                LocalDateTime.of(2026, 3, 25, 0, 0),
                null,
                1,
                LocalDateTime.of(2026, 3, 25, 10, 0),
                LocalDateTime.of(2026, 3, 25, 10, 0)
        );
    }

    private void mockToken(String token, boolean admin) {
        String uid = admin ? "admin-uid" : "user-uid";
        when(firebaseTokenVerifier.verify(token))
                .thenReturn(new FirebaseTokenClaims(
                        uid,
                        uid + "@sungkyul.ac.kr",
                        "google.com",
                        "provider-id",
                        admin ? "관리자" : "일반유저",
                        "https://example.com/profile.jpg"
                ));
        if (!admin) {
            when(memberRepository.findById(uid)).thenReturn(Optional.empty());
            return;
        }
        Member member = Member.create(uid, uid + "@sungkyul.ac.kr", "관리자", LocalDateTime.now());
        ReflectionTestUtils.setField(member, "isAdmin", true);
        when(memberRepository.findById(uid)).thenReturn(Optional.of(member));
    }
}
