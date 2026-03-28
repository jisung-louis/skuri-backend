package com.skuri.skuri_backend.domain.support.controller;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.support.dto.response.LegalDocumentResponse;
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
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseTokenVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LegalDocumentController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class LegalDocumentControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LegalDocumentService legalDocumentService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @Test
    void getLegalDocument_비인증정상요청_200() throws Exception {
        when(legalDocumentService.getLegalDocument("termsOfUse")).thenReturn(legalDocumentResponse());

        mockMvc.perform(get("/v1/legal-documents/termsOfUse"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("termsOfUse"))
                .andExpect(jsonPath("$.data.banner.iconKey").value("document"));
    }

    @Test
    void getLegalDocument_잘못된키_400() throws Exception {
        when(legalDocumentService.getLegalDocument("unknown"))
                .thenThrow(new BusinessException(ErrorCode.INVALID_REQUEST, "지원하지 않는 documentKey입니다."));

        mockMvc.perform(get("/v1/legal-documents/unknown"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
    }

    @Test
    void getLegalDocument_없는문서_404() throws Exception {
        when(legalDocumentService.getLegalDocument("privacyPolicy"))
                .thenThrow(new BusinessException(ErrorCode.LEGAL_DOCUMENT_NOT_FOUND));

        mockMvc.perform(get("/v1/legal-documents/privacyPolicy"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("LEGAL_DOCUMENT_NOT_FOUND"));
    }

    private LegalDocumentResponse legalDocumentResponse() {
        return new LegalDocumentResponse(
                "termsOfUse",
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
                )
        );
    }
}
