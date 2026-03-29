package com.skuri.skuri_backend.domain.admin.dashboard.controller;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.admin.dashboard.dto.response.AdminDashboardActivityResponse;
import com.skuri.skuri_backend.domain.admin.dashboard.dto.response.AdminDashboardRecentItemResponse;
import com.skuri.skuri_backend.domain.admin.dashboard.dto.response.AdminDashboardRecentItemType;
import com.skuri.skuri_backend.domain.admin.dashboard.dto.response.AdminDashboardSummaryResponse;
import com.skuri.skuri_backend.domain.admin.dashboard.service.AdminDashboardService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminDashboardController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class AdminDashboardControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminDashboardService adminDashboardService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @MockitoBean
    private com.skuri.skuri_backend.domain.member.repository.MemberRepository memberRepository;

    @Test
    void getSummary_관리자정상요청_200() throws Exception {
        mockToken("admin-token", true);
        when(adminDashboardService.getSummary()).thenReturn(new AdminDashboardSummaryResponse(
                12,
                4831,
                4,
                17,
                9,
                3,
                LocalDateTime.of(2026, 3, 29, 18, 0)
        ));

        mockMvc.perform(get("/v1/admin/dashboard/summary").header(AUTHORIZATION, "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.newMembersToday").value(12))
                .andExpect(jsonPath("$.data.totalMembers").value(4831))
                .andExpect(jsonPath("$.data.generatedAt").value("2026-03-29T18:00:00"));

        verify(adminDashboardService).getSummary();
    }

    @Test
    void getSummary_비관리자요청_403() throws Exception {
        mockToken("user-token", false);

        mockMvc.perform(get("/v1/admin/dashboard/summary").header(AUTHORIZATION, "Bearer user-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ADMIN_REQUIRED"));

        verifyNoInteractions(adminDashboardService);
    }

    @Test
    void getActivity_관리자정상요청_200() throws Exception {
        mockToken("admin-token", true);
        when(adminDashboardService.getActivity(30)).thenReturn(new AdminDashboardActivityResponse(
                30,
                "Asia/Seoul",
                List.of(
                        new AdminDashboardActivityResponse.ActivitySeriesItem(
                                LocalDate.of(2026, 2, 28),
                                3,
                                1,
                                0,
                                2
                        ),
                        new AdminDashboardActivityResponse.ActivitySeriesItem(
                                LocalDate.of(2026, 3, 29),
                                7,
                                2,
                                1,
                                4
                        )
                )
        ));

        mockMvc.perform(
                        get("/v1/admin/dashboard/activity")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .param("days", "30")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.days").value(30))
                .andExpect(jsonPath("$.data.timezone").value("Asia/Seoul"))
                .andExpect(jsonPath("$.data.series[0].date").value("2026-02-28"))
                .andExpect(jsonPath("$.data.series[1].partiesCreated").value(4));

        verify(adminDashboardService).getActivity(30);
    }

    @Test
    void getActivity_잘못된days_422() throws Exception {
        mockToken("admin-token", true);
        when(adminDashboardService.getActivity(15))
                .thenThrow(new BusinessException(ErrorCode.VALIDATION_ERROR, "days는 7 또는 30만 허용합니다."));

        mockMvc.perform(
                        get("/v1/admin/dashboard/activity")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .param("days", "15")
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("days는 7 또는 30만 허용합니다."));
    }

    @Test
    void getRecentItems_관리자정상요청_200() throws Exception {
        mockToken("admin-token", true);
        when(adminDashboardService.getRecentItems(10)).thenReturn(List.of(
                new AdminDashboardRecentItemResponse(
                        AdminDashboardRecentItemType.INQUIRY,
                        "inquiry-1",
                        "계정 문의",
                        "PENDING · member-1",
                        "PENDING",
                        LocalDateTime.of(2026, 3, 29, 17, 0)
                ),
                new AdminDashboardRecentItemResponse(
                        AdminDashboardRecentItemType.APP_NOTICE,
                        "notice-1",
                        "긴급 점검 안내",
                        "HIGH",
                        "PUBLISHED",
                        LocalDateTime.of(2026, 3, 29, 16, 30)
                )
        ));

        mockMvc.perform(get("/v1/admin/dashboard/recent-items").header(AUTHORIZATION, "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].type").value("INQUIRY"))
                .andExpect(jsonPath("$.data[0].subtitle").value("PENDING · member-1"))
                .andExpect(jsonPath("$.data[1].status").value("PUBLISHED"));

        verify(adminDashboardService).getRecentItems(10);
    }

    private void mockToken(String token, boolean isAdmin) {
        FirebaseTokenClaims claims = new FirebaseTokenClaims(
                "uid-" + token,
                "user@sungkyul.ac.kr",
                "google.com",
                "provider-id",
                "홍길동",
                null
        );
        when(firebaseTokenVerifier.verify(token)).thenReturn(claims);

        com.skuri.skuri_backend.domain.member.entity.Member member =
                com.skuri.skuri_backend.domain.member.entity.Member.create(
                        claims.uid(),
                        claims.email(),
                        claims.providerDisplayName(),
                        LocalDateTime.of(2026, 3, 1, 9, 0)
                );
        ReflectionTestUtils.setField(member, "isAdmin", isAdmin);
        when(memberRepository.findById(claims.uid())).thenReturn(Optional.of(member));
    }
}
