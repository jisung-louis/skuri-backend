package com.skuri.skuri_backend.domain.academic.controller;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.academic.dto.response.AcademicScheduleResponse;
import com.skuri.skuri_backend.domain.academic.entity.AcademicScheduleType;
import com.skuri.skuri_backend.domain.academic.service.AcademicScheduleService;
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

import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AcademicScheduleController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class AcademicScheduleControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AcademicScheduleService academicScheduleService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @Test
    void getAcademicSchedules_정상요청_200() throws Exception {
        mockValidToken();
        when(academicScheduleService.getSchedules(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 5, 1), true))
                .thenReturn(List.of(scheduleResponse()));

        mockMvc.perform(
                        get("/v1/academic-schedules")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .param("startDate", "2026-03-01")
                                .param("endDate", "2026-05-01")
                                .param("primary", "true")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("schedule-1"))
                .andExpect(jsonPath("$.data[0].type").value("MULTI"));
    }

    @Test
    void getAcademicSchedules_비인증요청_401() throws Exception {
        mockMvc.perform(get("/v1/academic-schedules"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void getAcademicSchedules_기간검증실패_422() throws Exception {
        mockValidToken();
        when(academicScheduleService.getSchedules(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 3, 1), null))
                .thenThrow(new BusinessException(ErrorCode.VALIDATION_ERROR, "startDate는 endDate보다 늦을 수 없습니다."));

        mockMvc.perform(
                        get("/v1/academic-schedules")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .param("startDate", "2026-05-01")
                                .param("endDate", "2026-03-01")
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    private void mockValidToken() {
        when(firebaseTokenVerifier.verify("valid-token"))
                .thenReturn(new FirebaseTokenClaims(
                        "firebase-uid",
                        "user@sungkyul.ac.kr",
                        "google.com",
                        "provider-id",
                        "테스터",
                        "https://example.com/profile.jpg"
                ));
    }

    private AcademicScheduleResponse scheduleResponse() {
        return new AcademicScheduleResponse(
                "schedule-1",
                "중간고사",
                LocalDate.of(2026, 4, 15),
                LocalDate.of(2026, 4, 21),
                AcademicScheduleType.MULTI,
                true,
                "2026학년도 1학기 중간고사"
        );
    }
}
