package com.skuri.skuri_backend.domain.support.controller;

import com.skuri.skuri_backend.common.dto.PageResponse;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.support.dto.request.UpdateInquiryStatusRequest;
import com.skuri.skuri_backend.domain.support.dto.response.AdminInquiryResponse;
import com.skuri.skuri_backend.domain.support.entity.InquiryStatus;
import com.skuri.skuri_backend.domain.support.entity.InquiryType;
import com.skuri.skuri_backend.domain.support.service.InquiryService;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InquiryAdminController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class InquiryAdminControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InquiryService inquiryService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @MockitoBean
    private MemberRepository memberRepository;

    @Test
    void getInquiries_관리자정상요청_200() throws Exception {
        mockToken("admin-token", true);
        when(inquiryService.getAdminInquiries(InquiryStatus.PENDING, 0, 20))
                .thenReturn(PageResponse.<AdminInquiryResponse>builder()
                        .content(java.util.List.of(adminInquiryResponse(InquiryStatus.PENDING, null)))
                        .page(0)
                        .size(20)
                        .totalElements(1)
                        .totalPages(1)
                        .hasNext(false)
                        .hasPrevious(false)
                        .build());

        mockMvc.perform(
                        get("/v1/admin/inquiries")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .param("status", "PENDING")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value("inquiry-1"))
                .andExpect(jsonPath("$.data.totalPages").value(1));
    }

    @Test
    void getInquiries_비관리자요청_403() throws Exception {
        mockToken("user-token", false);

        mockMvc.perform(get("/v1/admin/inquiries").header(AUTHORIZATION, "Bearer user-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ADMIN_REQUIRED"));
    }

    @Test
    void getInquiries_잘못된상태필터_400() throws Exception {
        mockToken("admin-token", true);

        mockMvc.perform(
                        get("/v1/admin/inquiries")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .param("status", "INVALID")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
    }

    @Test
    void updateInquiryStatus_관리자정상요청_200() throws Exception {
        mockToken("admin-token", true);
        when(inquiryService.updateInquiryStatus(eq("inquiry-1"), any(UpdateInquiryStatusRequest.class)))
                .thenReturn(adminInquiryResponse(InquiryStatus.RESOLVED, "재현 후 수정 배포 완료"));

        mockMvc.perform(
                        patch("/v1/admin/inquiries/inquiry-1/status")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "status": "RESOLVED",
                                          "memo": "재현 후 수정 배포 완료"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"));
    }

    @Test
    void updateInquiryStatus_없는문의_404() throws Exception {
        mockToken("admin-token", true);
        when(inquiryService.updateInquiryStatus(eq("missing"), any(UpdateInquiryStatusRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.INQUIRY_NOT_FOUND));

        mockMvc.perform(
                        patch("/v1/admin/inquiries/missing/status")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "status": "RESOLVED",
                                          "memo": "재현 후 수정 배포 완료"
                                        }
                                        """)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("INQUIRY_NOT_FOUND"));
    }

    @Test
    void updateInquiryStatus_허용되지않는상태전이_409() throws Exception {
        mockToken("admin-token", true);
        when(inquiryService.updateInquiryStatus(eq("inquiry-1"), any(UpdateInquiryStatusRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.INVALID_INQUIRY_STATUS_TRANSITION));

        mockMvc.perform(
                        patch("/v1/admin/inquiries/inquiry-1/status")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "status": "PENDING",
                                          "memo": null
                                        }
                                        """)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INQUIRY_STATUS_TRANSITION"));
    }

    private AdminInquiryResponse adminInquiryResponse(InquiryStatus status, String memo) {
        return new AdminInquiryResponse(
                "inquiry-1",
                "user-uid",
                InquiryType.BUG,
                "채팅 오류 문의",
                "채팅 진입 시 앱이 종료됩니다.",
                status,
                memo,
                "user@sungkyul.ac.kr",
                "스쿠리유저",
                "홍길동",
                "20201234",
                LocalDateTime.of(2026, 3, 5, 12, 0),
                LocalDateTime.of(2026, 3, 5, 12, 30)
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
