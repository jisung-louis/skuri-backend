package com.skuri.skuri_backend.domain.member.controller;

import com.skuri.skuri_backend.common.dto.PageResponse;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.member.dto.request.UpdateMemberAdminRoleRequest;
import com.skuri.skuri_backend.domain.member.dto.response.AdminMemberDetailResponse;
import com.skuri.skuri_backend.domain.member.dto.response.AdminMemberSummaryResponse;
import com.skuri.skuri_backend.domain.member.dto.response.MemberBankAccountResponse;
import com.skuri.skuri_backend.domain.member.dto.response.MemberNotificationSettingResponse;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.entity.MemberStatus;
import com.skuri.skuri_backend.domain.member.service.MemberAdminService;
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
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MemberAdminController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class MemberAdminControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberAdminService memberAdminService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @MockitoBean
    private com.skuri.skuri_backend.domain.member.repository.MemberRepository memberRepository;

    @Test
    void getAdminMembers_관리자정상요청_200() throws Exception {
        mockToken("admin-token", true);
        when(memberAdminService.getAdminMembers("홍길동", MemberStatus.ACTIVE, false, "컴퓨터공학과", 0, 20))
                .thenReturn(PageResponse.<AdminMemberSummaryResponse>builder()
                        .content(java.util.List.of(adminMemberSummaryResponse()))
                        .page(0)
                        .size(20)
                        .totalElements(1)
                        .totalPages(1)
                        .hasNext(false)
                        .hasPrevious(false)
                        .build());

        mockMvc.perform(
                        get("/v1/admin/members")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .param("query", "홍길동")
                                .param("status", "ACTIVE")
                                .param("isAdmin", "false")
                                .param("department", "컴퓨터공학과")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value("member-1"))
                .andExpect(jsonPath("$.data.content[0].isAdmin").value(false))
                .andExpect(jsonPath("$.data.content[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.content[0].bankAccount").doesNotExist());

        verify(memberAdminService).getAdminMembers("홍길동", MemberStatus.ACTIVE, false, "컴퓨터공학과", 0, 20);
    }

    @Test
    void getAdminMembers_비관리자요청_403() throws Exception {
        mockToken("user-token", false);

        mockMvc.perform(
                        get("/v1/admin/members")
                                .header(AUTHORIZATION, "Bearer user-token")
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ADMIN_REQUIRED"));
    }

    @Test
    void getAdminMembers_잘못된상태필터_400() throws Exception {
        mockToken("admin-token", true);

        mockMvc.perform(
                        get("/v1/admin/members")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .param("status", "INVALID")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));

        verifyNoInteractions(memberAdminService);
    }

    @Test
    void getAdminMember_관리자정상요청_200() throws Exception {
        mockToken("admin-token", true);
        when(memberAdminService.getAdminMember("member-1")).thenReturn(adminMemberDetailResponse(false));

        mockMvc.perform(
                        get("/v1/admin/members/member-1")
                                .header(AUTHORIZATION, "Bearer admin-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("member-1"))
                .andExpect(jsonPath("$.data.bankAccount.bankName").value("신한은행"))
                .andExpect(jsonPath("$.data.notificationSetting.noticeNotificationsDetail.academic").value(true));
    }

    @Test
    void getAdminMember_비관리자요청_403() throws Exception {
        mockToken("user-token", false);

        mockMvc.perform(
                        get("/v1/admin/members/member-1")
                                .header(AUTHORIZATION, "Bearer user-token")
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ADMIN_REQUIRED"));
    }

    @Test
    void getAdminMember_없는회원_404() throws Exception {
        mockToken("admin-token", true);
        when(memberAdminService.getAdminMember("missing"))
                .thenThrow(new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        mockMvc.perform(
                        get("/v1/admin/members/missing")
                                .header(AUTHORIZATION, "Bearer admin-token")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("MEMBER_NOT_FOUND"));
    }

    @Test
    void updateAdminRole_관리자정상요청_200() throws Exception {
        mockToken("admin-token", true);
        when(memberAdminService.updateAdminRole(eq("admin-uid"), eq("member-1"), any(UpdateMemberAdminRoleRequest.class)))
                .thenReturn(adminMemberDetailResponse(true));

        mockMvc.perform(
                        patch("/v1/admin/members/member-1/admin-role")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "isAdmin": true
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("member-1"))
                .andExpect(jsonPath("$.data.isAdmin").value(true));
    }

    @Test
    void updateAdminRole_자기자신이면_400() throws Exception {
        mockToken("admin-token", true);
        when(memberAdminService.updateAdminRole(eq("admin-uid"), eq("admin-uid"), any(UpdateMemberAdminRoleRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.SELF_ADMIN_ROLE_CHANGE_NOT_ALLOWED));

        mockMvc.perform(
                        patch("/v1/admin/members/admin-uid/admin-role")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "isAdmin": false
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("SELF_ADMIN_ROLE_CHANGE_NOT_ALLOWED"))
                .andExpect(jsonPath("$.message").value("자기 자신의 관리자 권한은 변경할 수 없습니다."));
    }

    @Test
    void updateAdminRole_비관리자요청_403() throws Exception {
        mockToken("user-token", false);

        mockMvc.perform(
                        patch("/v1/admin/members/member-1/admin-role")
                                .header(AUTHORIZATION, "Bearer user-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "isAdmin": true
                                        }
                                        """)
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ADMIN_REQUIRED"));
    }

    @Test
    void updateAdminRole_탈퇴회원이면_409() throws Exception {
        mockToken("admin-token", true);
        when(memberAdminService.updateAdminRole(eq("admin-uid"), eq("member-1"), any(UpdateMemberAdminRoleRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.CONFLICT, "탈퇴한 회원의 관리자 권한은 변경할 수 없습니다."));

        mockMvc.perform(
                        patch("/v1/admin/members/member-1/admin-role")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "isAdmin": false
                                        }
                                        """)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("탈퇴한 회원의 관리자 권한은 변경할 수 없습니다."));
    }

    @Test
    void updateAdminRole_isAdmin누락_422() throws Exception {
        mockToken("admin-token", true);

        mockMvc.perform(
                        patch("/v1/admin/members/member-1/admin-role")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                        }
                                        """)
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verifyNoInteractions(memberAdminService);
    }

    private AdminMemberSummaryResponse adminMemberSummaryResponse() {
        return new AdminMemberSummaryResponse(
                "member-1",
                "user@sungkyul.ac.kr",
                "스쿠리 유저",
                "홍길동",
                "2023112233",
                "컴퓨터공학과",
                false,
                LocalDateTime.of(2025, 3, 1, 9, 0),
                LocalDateTime.of(2026, 3, 29, 10, 5),
                MemberStatus.ACTIVE
        );
    }

    private AdminMemberDetailResponse adminMemberDetailResponse(boolean isAdmin) {
        return new AdminMemberDetailResponse(
                "member-1",
                "user@sungkyul.ac.kr",
                "스쿠리 유저",
                "홍길동",
                "2023112233",
                "컴퓨터공학과",
                "https://cdn.skuri.app/profiles/user-1.png",
                isAdmin,
                MemberStatus.ACTIVE,
                LocalDateTime.of(2025, 3, 1, 9, 0),
                LocalDateTime.of(2026, 3, 29, 10, 5),
                null,
                new MemberBankAccountResponse("신한은행", "110-123-456789", "홍길동", false),
                new MemberNotificationSettingResponse(
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        false,
                        Map.of("academic", true, "event", false)
                )
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
