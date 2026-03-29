package com.skuri.skuri_backend.domain.academic.controller;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.academic.dto.request.AdminBulkCoursesRequest;
import com.skuri.skuri_backend.domain.academic.dto.response.AdminBulkCoursesResponse;
import com.skuri.skuri_backend.domain.academic.service.CourseService;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CourseAdminController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class CourseAdminControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseService courseService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @MockitoBean
    private MemberRepository memberRepository;

    @Test
    void bulkUpsertCourses_관리자정상요청_200() throws Exception {
        mockToken("admin-token", true);
        when(courseService.bulkUpsertCourses(any(AdminBulkCoursesRequest.class)))
                .thenReturn(new AdminBulkCoursesResponse("2026-1", 120, 5, 3));

        mockMvc.perform(
                        post("/v1/admin/courses/bulk")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content(validBulkRequest())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.created").value(120));
    }

    @Test
    void bulkUpsertCourses_공식온라인강의정상요청_200() throws Exception {
        mockToken("admin-token", true);
        when(courseService.bulkUpsertCourses(any(AdminBulkCoursesRequest.class)))
                .thenReturn(new AdminBulkCoursesResponse("2026-1", 1, 0, 0));

        mockMvc.perform(
                        post("/v1/admin/courses/bulk")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content(validOnlineBulkRequest())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.created").value(1));
    }

    @Test
    void bulkUpsertCourses_비관리자요청_403() throws Exception {
        mockToken("user-token", false);

        mockMvc.perform(
                        post("/v1/admin/courses/bulk")
                                .header(AUTHORIZATION, "Bearer user-token")
                                .contentType(APPLICATION_JSON)
                                .content(validBulkRequest())
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ADMIN_REQUIRED"));
    }

    @Test
    void bulkUpsertCourses_충돌_409() throws Exception {
        mockToken("admin-token", true);
        when(courseService.bulkUpsertCourses(any(AdminBulkCoursesRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.CONFLICT, "강의 bulk 처리 중 충돌이 발생했습니다."));

        mockMvc.perform(
                        post("/v1/admin/courses/bulk")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content(validBulkRequest())
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONFLICT"));
    }

    @Test
    void bulkUpsertCourses_요청검증실패_422() throws Exception {
        mockToken("admin-token", true);

        mockMvc.perform(
                        post("/v1/admin/courses/bulk")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "semester": "",
                                          "courses": []
                                        }
                                        """)
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void bulkUpsertCourses_온라인강의에schedule이있으면_422() throws Exception {
        mockToken("admin-token", true);
        when(courseService.bulkUpsertCourses(any(AdminBulkCoursesRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.VALIDATION_ERROR, "온라인 강의는 schedule을 비워야 합니다."));

        mockMvc.perform(
                        post("/v1/admin/courses/bulk")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content(onlineScheduleNotEmptyBulkRequest())
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void bulkUpsertCourses_isOnline생략하위호환요청_200() throws Exception {
        mockToken("admin-token", true);
        when(courseService.bulkUpsertCourses(any(AdminBulkCoursesRequest.class)))
                .thenReturn(new AdminBulkCoursesResponse("2026-1", 1, 0, 0));

        mockMvc.perform(
                        post("/v1/admin/courses/bulk")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content(legacyBulkRequestWithoutIsOnline())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.created").value(1));
    }

    @Test
    void bulkUpsertCourses_null강의항목_422() throws Exception {
        mockToken("admin-token", true);

        mockMvc.perform(
                        post("/v1/admin/courses/bulk")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "semester": "2026-1",
                                          "courses": [null]
                                        }
                                        """)
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void bulkUpsertCourses_nullSchedule항목_422() throws Exception {
        mockToken("admin-token", true);

        mockMvc.perform(
                        post("/v1/admin/courses/bulk")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "semester": "2026-1",
                                          "courses": [
                                            {
                                              "code": "01255",
                                              "division": "001",
                                              "name": "민법총칙",
                                              "credits": 3,
                                              "professor": "문상혁",
                                              "department": "법학과",
                                              "grade": 2,
                                              "category": "전공선택",
                                              "location": "영401",
                                              "note": null,
                                              "schedule": [null]
                                            }
                                          ]
                                        }
                                        """)
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void bulkUpsertCourses_선택문자열길이초과_422() throws Exception {
        mockToken("admin-token", true);
        String tooLongProfessor = "a".repeat(51);

        mockMvc.perform(
                        post("/v1/admin/courses/bulk")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "semester": "2026-1",
                                          "courses": [
                                            {
                                              "code": "01255",
                                              "division": "001",
                                              "name": "민법총칙",
                                              "credits": 3,
                                              "professor": "%s",
                                              "department": "법학과",
                                              "grade": 2,
                                              "category": "전공선택",
                                              "location": "영401",
                                              "note": null,
                                              "schedule": [
                                                { "dayOfWeek": 1, "startPeriod": 3, "endPeriod": 4 }
                                              ]
                                            }
                                          ]
                                        }
                                        """.formatted(tooLongProfessor))
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void deleteCourses_관리자정상요청_200() throws Exception {
        mockToken("admin-token", true);
        when(courseService.deleteSemesterCourses("2026-1"))
                .thenReturn(new AdminBulkCoursesResponse("2026-1", 0, 0, 125));

        mockMvc.perform(
                        delete("/v1/admin/courses")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .param("semester", "2026-1")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deleted").value(125));
    }

    @Test
    void deleteCourses_비관리자요청_403() throws Exception {
        mockToken("user-token", false);

        mockMvc.perform(
                        delete("/v1/admin/courses")
                                .header(AUTHORIZATION, "Bearer user-token")
                                .param("semester", "2026-1")
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ADMIN_REQUIRED"));
    }

    @Test
    void deleteCourses_학기누락_400() throws Exception {
        mockToken("admin-token", true);

        mockMvc.perform(
                        delete("/v1/admin/courses")
                                .header(AUTHORIZATION, "Bearer admin-token")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
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

    private String validBulkRequest() {
        return """
                {
                  "semester": "2026-1",
                  "courses": [
                    {
                      "code": "01255",
                      "division": "001",
                      "name": "민법총칙",
                      "credits": 3,
                      "professor": "문상혁",
                      "department": "법학과",
                      "grade": 2,
                      "category": "전공선택",
                      "location": "영401",
                      "note": null,
                      "isOnline": false,
                      "schedule": [
                        { "dayOfWeek": 1, "startPeriod": 3, "endPeriod": 4 }
                      ]
                    }
                  ]
                }
                """;
    }

    private String validOnlineBulkRequest() {
        return """
                {
                  "semester": "2026-1",
                  "courses": [
                    {
                      "grade": 1,
                      "category": "교양선택",
                      "code": "20797",
                      "division": "001",
                      "name": "사랑의인문학(KCU온라인강좌)",
                      "credits": 3,
                      "professor": null,
                      "location": null,
                      "department": "교양",
                      "note": null,
                      "isOnline": true,
                      "schedule": []
                    }
                  ]
                }
                """;
    }

    private String onlineScheduleNotEmptyBulkRequest() {
        return """
                {
                  "semester": "2026-1",
                  "courses": [
                    {
                      "grade": 1,
                      "category": "교양선택",
                      "code": "20797",
                      "division": "001",
                      "name": "사랑의인문학(KCU온라인강좌)",
                      "credits": 3,
                      "professor": null,
                      "location": "온라인",
                      "department": "교양",
                      "note": null,
                      "isOnline": true,
                      "schedule": [
                        { "dayOfWeek": 1, "startPeriod": 1, "endPeriod": 1 }
                      ]
                    }
                  ]
                }
                """;
    }

    private String legacyBulkRequestWithoutIsOnline() {
        return """
                {
                  "semester": "2026-1",
                  "courses": [
                    {
                      "code": "01255",
                      "division": "001",
                      "name": "민법총칙",
                      "credits": 3,
                      "professor": "문상혁",
                      "department": "법학과",
                      "grade": 2,
                      "category": "전공선택",
                      "location": "영401",
                      "note": null,
                      "schedule": [
                        { "dayOfWeek": 1, "startPeriod": 3, "endPeriod": 4 }
                      ]
                    }
                  ]
                }
                """;
    }
}
