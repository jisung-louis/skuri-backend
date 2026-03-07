package com.skuri.skuri_backend.domain.academic.controller;

import com.skuri.skuri_backend.common.dto.PageResponse;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.academic.dto.response.CourseScheduleResponse;
import com.skuri.skuri_backend.domain.academic.dto.response.CourseSummaryResponse;
import com.skuri.skuri_backend.domain.academic.service.CourseService;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CourseController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class CourseControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseService courseService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @Test
    void getCourses_정상요청_200() throws Exception {
        mockValidToken();
        when(courseService.getCourses("2026-1", "법학과", "문상혁", "민법", 1, 2, null, null))
                .thenReturn(PageResponse.from(new PageImpl<>(List.of(courseResponse()))));

        mockMvc.perform(
                        get("/v1/courses")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .param("semester", "2026-1")
                                .param("department", "법학과")
                                .param("professor", "문상혁")
                                .param("search", "민법")
                                .param("dayOfWeek", "1")
                                .param("grade", "2")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value("course-1"))
                .andExpect(jsonPath("$.data.content[0].credits").value(3));
    }

    @Test
    void getCourses_비인증요청_401() throws Exception {
        mockMvc.perform(get("/v1/courses"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void getCourses_필터검증실패_422() throws Exception {
        mockValidToken();
        when(courseService.getCourses(null, null, null, null, 0, null, null, null))
                .thenThrow(new BusinessException(ErrorCode.VALIDATION_ERROR, "dayOfWeek는 1 이상 5 이하여야 합니다."));

        mockMvc.perform(
                        get("/v1/courses")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .param("dayOfWeek", "0")
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

    private CourseSummaryResponse courseResponse() {
        return new CourseSummaryResponse(
                "course-1",
                "2026-1",
                "01255",
                "001",
                "민법총칙",
                3,
                "문상혁",
                "법학과",
                2,
                "전공선택",
                "영401",
                null,
                List.of(
                        new CourseScheduleResponse(1, 3, 4),
                        new CourseScheduleResponse(3, 3, 4)
                )
        );
    }
}
