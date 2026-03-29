package com.skuri.skuri_backend.domain.taxiparty.controller;

import com.skuri.skuri_backend.common.dto.PageResponse;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.taxiparty.constant.AdminPartyStatusAction;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.AdminPartyDetailResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.AdminPartyLeaderResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.AdminPartySummaryResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.MemberSettlementResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyLocationResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyMemberResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyStatusResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.SettlementAccountResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.SettlementSummaryResponse;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus;
import com.skuri.skuri_backend.domain.taxiparty.entity.SettlementStatus;
import com.skuri.skuri_backend.domain.taxiparty.service.TaxiPartyAdminService;
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
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PartyAdminController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class PartyAdminControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaxiPartyAdminService taxiPartyAdminService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @MockitoBean
    private com.skuri.skuri_backend.domain.member.repository.MemberRepository memberRepository;

    @Test
    void getAdminParties_관리자정상요청_200() throws Exception {
        mockToken("admin-token", true);
        when(taxiPartyAdminService.getAdminParties(
                PartyStatus.OPEN,
                LocalDate.of(2026, 3, 29),
                "안양역",
                0,
                20
        )).thenReturn(PageResponse.<AdminPartySummaryResponse>builder()
                .content(List.of(new AdminPartySummaryResponse(
                        "party-1",
                        PartyStatus.OPEN,
                        "leader-1",
                        "스쿠리 유저",
                        "성결대학교 -> 안양역",
                        LocalDateTime.of(2026, 3, 29, 18, 30),
                        2,
                        4,
                        LocalDateTime.of(2026, 3, 29, 12, 0)
                )))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .hasNext(false)
                .hasPrevious(false)
                .build());

        mockMvc.perform(
                        get("/v1/admin/parties")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .param("status", "OPEN")
                                .param("departureDate", "2026-03-29")
                                .param("query", "안양역")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value("party-1"))
                .andExpect(jsonPath("$.data.content[0].leaderNickname").value("스쿠리 유저"))
                .andExpect(jsonPath("$.data.content[0].routeSummary").value("성결대학교 -> 안양역"));

        verify(taxiPartyAdminService).getAdminParties(
                PartyStatus.OPEN,
                LocalDate.of(2026, 3, 29),
                "안양역",
                0,
                20
        );
    }

    @Test
    void getAdminParty_관리자정상요청_200() throws Exception {
        mockToken("admin-token", true);
        when(taxiPartyAdminService.getAdminParty("party-1")).thenReturn(adminPartyDetailResponse());

        mockMvc.perform(
                        get("/v1/admin/parties/party-1")
                                .header(AUTHORIZATION, "Bearer admin-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("party-1"))
                .andExpect(jsonPath("$.data.pendingJoinRequestCount").value(2))
                .andExpect(jsonPath("$.data.settlementStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.chatRoomId").value("party:party-1"));
    }

    @Test
    void getAdminParties_비관리자요청_403() throws Exception {
        mockToken("user-token", false);

        mockMvc.perform(
                        get("/v1/admin/parties")
                                .header(AUTHORIZATION, "Bearer user-token")
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ADMIN_REQUIRED"));
    }

    @Test
    void updatePartyStatus_정상요청_200() throws Exception {
        mockToken("admin-token", true);
        when(taxiPartyAdminService.updatePartyStatus("party-1", AdminPartyStatusAction.CLOSE))
                .thenReturn(new PartyStatusResponse("party-1", PartyStatus.CLOSED, null));

        mockMvc.perform(
                        patch("/v1/admin/parties/party-1/status")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "action": "CLOSE"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("party-1"))
                .andExpect(jsonPath("$.data.status").value("CLOSED"))
                .andExpect(jsonPath("$.data.endReason").doesNotExist());
    }

    @Test
    void updatePartyStatus_허용되지않는전이면_409() throws Exception {
        mockToken("admin-token", true);
        when(taxiPartyAdminService.updatePartyStatus("party-1", AdminPartyStatusAction.END))
                .thenThrow(new BusinessException(ErrorCode.INVALID_PARTY_STATE_TRANSITION, "ARRIVED 상태에서만 강제 종료할 수 있습니다."));

        mockMvc.perform(
                        patch("/v1/admin/parties/party-1/status")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "action": "END"
                                        }
                                        """)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INVALID_PARTY_STATE_TRANSITION"))
                .andExpect(jsonPath("$.message").value("ARRIVED 상태에서만 강제 종료할 수 있습니다."));
    }

    private AdminPartyDetailResponse adminPartyDetailResponse() {
        return new AdminPartyDetailResponse(
                "party-1",
                PartyStatus.ARRIVED,
                null,
                "leader-1",
                "스쿠리 유저",
                new AdminPartyLeaderResponse("leader-1", "스쿠리 유저", "https://cdn.skuri.app/profiles/leader.png"),
                "성결대학교 -> 안양역",
                new PartyLocationResponse("성결대학교", 37.38, 126.93),
                new PartyLocationResponse("안양역", 37.40, 126.92),
                LocalDateTime.of(2026, 3, 29, 18, 30),
                3,
                4,
                List.of(
                        new PartyMemberResponse("leader-1", "스쿠리 유저", "https://cdn.skuri.app/profiles/leader.png", true, LocalDateTime.of(2026, 3, 29, 12, 0)),
                        new PartyMemberResponse("member-1", "김철수", null, false, LocalDateTime.of(2026, 3, 29, 12, 5))
                ),
                List.of("빠른출발"),
                "정문 앞 택시승강장 집합",
                2,
                SettlementStatus.PENDING,
                new SettlementSummaryResponse(
                        SettlementStatus.PENDING,
                        15000,
                        3,
                        5000,
                        List.of("member-1", "member-2"),
                        new SettlementAccountResponse("카카오뱅크", "3333-01-1234567", "홍*동", true),
                        List.of(new MemberSettlementResponse("member-1", "김철수", false, null, false, null))
                ),
                "party:party-1",
                LocalDateTime.of(2026, 3, 29, 12, 0),
                LocalDateTime.of(2026, 3, 29, 18, 30),
                null
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
