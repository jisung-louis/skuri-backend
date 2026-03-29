package com.skuri.skuri_backend.domain.academic.controller;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.academic.dto.request.CreateAcademicScheduleRequest;
import com.skuri.skuri_backend.domain.academic.dto.request.UpdateAcademicScheduleRequest;
import com.skuri.skuri_backend.domain.academic.dto.response.AcademicScheduleResponse;
import com.skuri.skuri_backend.domain.academic.dto.response.AdminBulkAcademicSchedulesResponse;
import com.skuri.skuri_backend.domain.academic.entity.AcademicScheduleType;
import com.skuri.skuri_backend.domain.academic.service.AcademicScheduleService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AcademicScheduleAdminController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class AcademicScheduleAdminControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AcademicScheduleService academicScheduleService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @MockitoBean
    private MemberRepository memberRepository;

    @Test
    void createAcademicSchedule_관리자정상요청_201() throws Exception {
        mockToken("admin-token", true);
        when(academicScheduleService.createSchedule(any(CreateAcademicScheduleRequest.class))).thenReturn(scheduleResponse());

        mockMvc.perform(
                        post("/v1/admin/academic-schedules")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content(validRequestBody())
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value("schedule-1"));
    }

    @Test
    void createAcademicSchedule_비관리자요청_403() throws Exception {
        mockToken("user-token", false);

        mockMvc.perform(
                        post("/v1/admin/academic-schedules")
                                .header(AUTHORIZATION, "Bearer user-token")
                                .contentType(APPLICATION_JSON)
                                .content(validRequestBody())
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ADMIN_REQUIRED"));
    }

    @Test
    void createAcademicSchedule_요청검증실패_422() throws Exception {
        mockToken("admin-token", true);

        mockMvc.perform(
                        post("/v1/admin/academic-schedules")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "",
                                          "startDate": null,
                                          "endDate": null,
                                          "type": null,
                                          "isPrimary": null
                                        }
                                        """)
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void bulkSyncAcademicSchedules_관리자정상요청_200() throws Exception {
        mockToken("admin-token", true);
        when(academicScheduleService.bulkSyncSchedules(any()))
                .thenReturn(new AdminBulkAcademicSchedulesResponse(
                        LocalDate.of(2026, 3, 1),
                        LocalDate.of(2027, 2, 28),
                        12,
                        37,
                        5
                ));

        mockMvc.perform(
                        put("/v1/admin/academic-schedules/bulk")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content(validBulkRequestBody())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scopeStartDate").value("2026-03-01"))
                .andExpect(jsonPath("$.data.created").value(12))
                .andExpect(jsonPath("$.data.updated").value(37))
                .andExpect(jsonPath("$.data.deleted").value(5));
    }

    @Test
    void bulkSyncAcademicSchedules_비관리자요청_403() throws Exception {
        mockToken("user-token", false);

        mockMvc.perform(
                        put("/v1/admin/academic-schedules/bulk")
                                .header(AUTHORIZATION, "Bearer user-token")
                                .contentType(APPLICATION_JSON)
                                .content(validBulkRequestBody())
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ADMIN_REQUIRED"));
    }

    @Test
    void bulkSyncAcademicSchedules_잘못된scope_422() throws Exception {
        mockToken("admin-token", true);
        when(academicScheduleService.bulkSyncSchedules(any()))
                .thenThrow(new BusinessException(ErrorCode.VALIDATION_ERROR, "scopeStartDate는 scopeEndDate보다 늦을 수 없습니다."));

        mockMvc.perform(
                        put("/v1/admin/academic-schedules/bulk")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content(validBulkRequestBody())
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("scopeStartDate는 scopeEndDate보다 늦을 수 없습니다."));
    }

    @Test
    void bulkSyncAcademicSchedules_자연키중복_422() throws Exception {
        mockToken("admin-token", true);
        when(academicScheduleService.bulkSyncSchedules(any()))
                .thenThrow(new BusinessException(
                        ErrorCode.VALIDATION_ERROR,
                        "같은 title/startDate/endDate/type 조합이 요청에 중복되었습니다: 입학식 / 개강|2026-03-03|2026-03-03|SINGLE"
                ));

        mockMvc.perform(
                        put("/v1/admin/academic-schedules/bulk")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content(validBulkRequestBody())
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("같은 title/startDate/endDate/type 조합이 요청에 중복되었습니다: 입학식 / 개강|2026-03-03|2026-03-03|SINGLE"));
    }

    @Test
    void bulkSyncAcademicSchedules_scope밖일정포함_422() throws Exception {
        mockToken("admin-token", true);
        when(academicScheduleService.bulkSyncSchedules(any()))
                .thenThrow(new BusinessException(
                        ErrorCode.VALIDATION_ERROR,
                        "모든 일정은 scopeStartDate와 scopeEndDate 범위 안에 있어야 합니다."
                ));

        mockMvc.perform(
                        put("/v1/admin/academic-schedules/bulk")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content(validBulkRequestBody())
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("모든 일정은 scopeStartDate와 scopeEndDate 범위 안에 있어야 합니다."));
    }

    @Test
    void updateAcademicSchedule_없는일정_404() throws Exception {
        mockToken("admin-token", true);
        when(academicScheduleService.updateSchedule(eq("missing"), any(UpdateAcademicScheduleRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.ACADEMIC_SCHEDULE_NOT_FOUND, "학사 일정을 찾을 수 없습니다."));

        mockMvc.perform(
                        put("/v1/admin/academic-schedules/missing")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content(validRequestBody())
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ACADEMIC_SCHEDULE_NOT_FOUND"));
    }

    @Test
    void updateAcademicSchedule_관리자정상요청_200() throws Exception {
        mockToken("admin-token", true);
        when(academicScheduleService.updateSchedule(eq("schedule-1"), any(UpdateAcademicScheduleRequest.class)))
                .thenReturn(scheduleResponse());

        mockMvc.perform(
                        put("/v1/admin/academic-schedules/schedule-1")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content(validRequestBody())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("schedule-1"))
                .andExpect(jsonPath("$.data.title").value("중간고사"));
    }

    @Test
    void deleteAcademicSchedule_관리자정상요청_200() throws Exception {
        mockToken("admin-token", true);

        mockMvc.perform(delete("/v1/admin/academic-schedules/schedule-1").header(AUTHORIZATION, "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void deleteAcademicSchedule_비관리자요청_403() throws Exception {
        mockToken("user-token", false);

        mockMvc.perform(delete("/v1/admin/academic-schedules/schedule-1").header(AUTHORIZATION, "Bearer user-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ADMIN_REQUIRED"));
    }

    private void mockToken(String token, boolean admin) {
        String uid = admin ? "admin-uid" : "user-uid";
        when(firebaseTokenVerifier.verify(token))
                .thenReturn(new FirebaseTokenClaims(
                        uid,
                        uid + "@sungkyul.ac.kr",
                        "google.com",
                        "provider-id",
                        "테스터",
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

    private String validRequestBody() {
        return """
                {
                  "title": "중간고사",
                  "startDate": "2026-04-15",
                  "endDate": "2026-04-21",
                  "type": "MULTI",
                  "isPrimary": true,
                  "description": "2026학년도 1학기 중간고사"
                }
                """;
    }

    private String validBulkRequestBody() {
        return """
                {
                  "scopeStartDate": "2026-03-01",
                  "scopeEndDate": "2027-02-28",
                  "schedules": [
                    {
                      "title": "입학식 / 개강",
                      "startDate": "2026-03-03",
                      "endDate": "2026-03-03",
                      "type": "single",
                      "description": "정상수업",
                      "isPrimary": true
                    },
                    {
                      "title": "수강신청 변경기간",
                      "startDate": "2026-03-04",
                      "endDate": "2026-03-09",
                      "type": "MULTI",
                      "description": null,
                      "isPrimary": true
                    }
                  ]
                }
                """;
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
