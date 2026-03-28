package com.skuri.skuri_backend.domain.support.controller;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.support.dto.response.LegalDocumentAdminResponse;
import com.skuri.skuri_backend.domain.support.dto.response.LegalDocumentAdminSummaryResponse;
import com.skuri.skuri_backend.domain.support.entity.LegalDocumentBannerIconKey;
import com.skuri.skuri_backend.domain.support.entity.LegalDocumentBannerLineTone;
import com.skuri.skuri_backend.domain.support.entity.LegalDocumentBannerTone;
import com.skuri.skuri_backend.domain.support.model.LegalDocumentBanner;
import com.skuri.skuri_backend.domain.support.model.LegalDocumentBannerLine;
import com.skuri.skuri_backend.domain.support.model.LegalDocumentSection;
import com.skuri.skuri_backend.domain.support.service.LegalDocumentService;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LegalDocumentAdminController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class LegalDocumentAdminControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LegalDocumentService legalDocumentService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @MockitoBean
    private MemberRepository memberRepository;

    @Test
    void getLegalDocuments_관리자정상요청_200() throws Exception {
        mockToken("admin-token", true);
        when(legalDocumentService.getAdminLegalDocuments()).thenReturn(List.of(
                new LegalDocumentAdminSummaryResponse("termsOfUse", "이용약관", true, LocalDateTime.of(2026, 3, 28, 10, 0))
        ));

        mockMvc.perform(get("/v1/admin/legal-documents").header(AUTHORIZATION, "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("termsOfUse"));
    }

    @Test
    void getLegalDocuments_비관리자요청_403() throws Exception {
        mockToken("user-token", false);

        mockMvc.perform(get("/v1/admin/legal-documents").header(AUTHORIZATION, "Bearer user-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ADMIN_REQUIRED"));
    }

    @Test
    void getLegalDocument_관리자정상요청_200() throws Exception {
        mockToken("admin-token", true);
        when(legalDocumentService.getAdminLegalDocument("privacyPolicy")).thenReturn(adminResponse("privacyPolicy"));

        mockMvc.perform(get("/v1/admin/legal-documents/privacyPolicy").header(AUTHORIZATION, "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("privacyPolicy"));
    }

    @Test
    void getLegalDocument_없는문서_404() throws Exception {
        mockToken("admin-token", true);
        when(legalDocumentService.getAdminLegalDocument("privacyPolicy"))
                .thenThrow(new BusinessException(ErrorCode.LEGAL_DOCUMENT_NOT_FOUND));

        mockMvc.perform(get("/v1/admin/legal-documents/privacyPolicy").header(AUTHORIZATION, "Bearer admin-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("LEGAL_DOCUMENT_NOT_FOUND"));
    }

    @Test
    void upsertLegalDocument_관리자정상요청_200() throws Exception {
        mockToken("admin-token", true);
        when(legalDocumentService.upsertLegalDocument(eq("termsOfUse"), any())).thenReturn(adminResponse("termsOfUse"));

        mockMvc.perform(
                        put("/v1/admin/legal-documents/termsOfUse")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content(validRequest())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("termsOfUse"))
                .andExpect(jsonPath("$.data.isActive").value(true));
    }

    @Test
    void upsertLegalDocument_검증실패_422() throws Exception {
        mockToken("admin-token", true);

        mockMvc.perform(
                        put("/v1/admin/legal-documents/termsOfUse")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "",
                                          "banner": null,
                                          "sections": [],
                                          "footerLines": null,
                                          "isActive": null
                                        }
                                        """)
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void upsertLegalDocument_sections에Null항목이있으면_422() throws Exception {
        mockToken("admin-token", true);

        mockMvc.perform(
                        put("/v1/admin/legal-documents/termsOfUse")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "이용약관",
                                          "banner": {
                                            "iconKey": "document",
                                            "lines": [
                                              {
                                                "text": "시행일: 2025년 3월 1일 · 최종 수정: 2025년 3월 1일",
                                                "tone": "primary"
                                              }
                                            ],
                                            "title": "SKURI 이용약관",
                                            "tone": "green"
                                          },
                                          "sections": [null],
                                          "footerLines": [
                                            "본 약관에 대한 문의는",
                                            "앱 내 문의하기를 이용해 주세요."
                                          ],
                                          "isActive": true
                                        }
                                        """)
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verifyNoInteractions(legalDocumentService);
    }

    @Test
    void upsertLegalDocument_bannerLines에Null항목이있으면_422() throws Exception {
        mockToken("admin-token", true);

        mockMvc.perform(
                        put("/v1/admin/legal-documents/termsOfUse")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "이용약관",
                                          "banner": {
                                            "iconKey": "document",
                                            "lines": [null],
                                            "title": "SKURI 이용약관",
                                            "tone": "green"
                                          },
                                          "sections": [
                                            {
                                              "id": "article-01",
                                              "paragraphs": [
                                                "이 약관은 회사와 회원 간의 권리, 의무를 규정합니다."
                                              ],
                                              "title": "제1조(목적)"
                                            }
                                          ],
                                          "footerLines": [
                                            "본 약관에 대한 문의는",
                                            "앱 내 문의하기를 이용해 주세요."
                                          ],
                                          "isActive": true
                                        }
                                        """)
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verifyNoInteractions(legalDocumentService);
    }

    @Test
    void deleteLegalDocument_관리자정상요청_200() throws Exception {
        mockToken("admin-token", true);
        when(legalDocumentService.deleteLegalDocument("termsOfUse"))
                .thenReturn(new com.skuri.skuri_backend.domain.support.dto.response.LegalDocumentDeleteResponse("termsOfUse"));

        mockMvc.perform(delete("/v1/admin/legal-documents/termsOfUse").header(AUTHORIZATION, "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("termsOfUse"));
    }

    @Test
    void deleteLegalDocument_없는문서_404() throws Exception {
        mockToken("admin-token", true);
        doThrow(new BusinessException(ErrorCode.LEGAL_DOCUMENT_NOT_FOUND))
                .when(legalDocumentService).deleteLegalDocument("termsOfUse");

        mockMvc.perform(delete("/v1/admin/legal-documents/termsOfUse").header(AUTHORIZATION, "Bearer admin-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("LEGAL_DOCUMENT_NOT_FOUND"));
    }

    private String validRequest() {
        return """
                {
                  "title": "이용약관",
                  "banner": {
                    "iconKey": "document",
                    "lines": [
                      {
                        "text": "시행일: 2025년 3월 1일 · 최종 수정: 2025년 3월 1일",
                        "tone": "primary"
                      }
                    ],
                    "title": "SKURI 이용약관",
                    "tone": "green"
                  },
                  "sections": [
                    {
                      "id": "article-01",
                      "paragraphs": [
                        "이 약관은 회사와 회원 간의 권리, 의무를 규정합니다."
                      ],
                      "title": "제1조(목적)"
                    }
                  ],
                  "footerLines": [
                    "본 약관에 대한 문의는",
                    "앱 내 문의하기를 이용해 주세요."
                  ],
                  "isActive": true
                }
                """;
    }

    private LegalDocumentAdminResponse adminResponse(String id) {
        return new LegalDocumentAdminResponse(
                id,
                "이용약관",
                new LegalDocumentBanner(
                        LegalDocumentBannerIconKey.DOCUMENT,
                        List.of(new LegalDocumentBannerLine(
                                "시행일: 2025년 3월 1일 · 최종 수정: 2025년 3월 1일",
                                LegalDocumentBannerLineTone.PRIMARY
                        )),
                        "SKURI 이용약관",
                        LegalDocumentBannerTone.GREEN
                ),
                List.of(new LegalDocumentSection(
                        "article-01",
                        List.of("이 약관은 회사와 회원 간의 권리, 의무를 규정합니다."),
                        "제1조(목적)"
                )),
                List.of(
                        "본 약관에 대한 문의는",
                        "앱 내 문의하기를 이용해 주세요."
                ),
                true,
                LocalDateTime.of(2026, 3, 28, 10, 0),
                LocalDateTime.of(2026, 3, 28, 10, 0)
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
