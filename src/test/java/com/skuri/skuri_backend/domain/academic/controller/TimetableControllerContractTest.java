package com.skuri.skuri_backend.domain.academic.controller;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.academic.dto.request.AddMyTimetableCourseRequest;
import com.skuri.skuri_backend.domain.academic.dto.response.CourseScheduleResponse;
import com.skuri.skuri_backend.domain.academic.dto.response.TimetableCourseResponse;
import com.skuri.skuri_backend.domain.academic.dto.response.TimetableSlotResponse;
import com.skuri.skuri_backend.domain.academic.dto.response.UserTimetableResponse;
import com.skuri.skuri_backend.domain.academic.service.TimetableService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TimetableController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class TimetableControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TimetableService timetableService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @Test
    void getMyTimetable_정상요청_200() throws Exception {
        mockValidToken();
        when(timetableService.getMyTimetable("firebase-uid", "2026-1")).thenReturn(timetableResponse());

        mockMvc.perform(
                        get("/v1/timetables/my")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .param("semester", "2026-1")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("timetable-1"))
                .andExpect(jsonPath("$.data.slots[0].courseName").value("민법총칙"))
                .andExpect(jsonPath("$.data.courses[0].color").doesNotExist())
                .andExpect(jsonPath("$.data.slots[0].color").doesNotExist());
    }

    @Test
    void getMyTimetable_비인증요청_401() throws Exception {
        mockMvc.perform(get("/v1/timetables/my"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void addCourse_정상요청_200() throws Exception {
        mockValidToken();
        when(timetableService.addCourse(eq("firebase-uid"), any(AddMyTimetableCourseRequest.class)))
                .thenReturn(timetableResponse());

        mockMvc.perform(
                        post("/v1/timetables/my/courses")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "courseId": "course-1",
                                          "semester": "2026-1"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courseCount").value(1))
                .andExpect(jsonPath("$.data.courses[0].id").value("course-1"))
                .andExpect(jsonPath("$.data.slots[0].courseName").value("민법총칙"))
                .andExpect(jsonPath("$.data.courses[0].color").doesNotExist())
                .andExpect(jsonPath("$.data.slots[0].color").doesNotExist());
    }

    @Test
    void addCourse_중복강의_409() throws Exception {
        mockValidToken();
        when(timetableService.addCourse(eq("firebase-uid"), any(AddMyTimetableCourseRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.COURSE_ALREADY_IN_TIMETABLE));

        mockMvc.perform(
                        post("/v1/timetables/my/courses")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "courseId": "course-1",
                                          "semester": "2026-1"
                                        }
                                        """)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("COURSE_ALREADY_IN_TIMETABLE"));
    }

    @Test
    void addCourse_요청검증실패_422() throws Exception {
        mockValidToken();

        mockMvc.perform(
                        post("/v1/timetables/my/courses")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "courseId": "",
                                          "semester": ""
                                        }
                                        """)
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void deleteCourse_강의없음_404() throws Exception {
        mockValidToken();
        when(timetableService.removeCourse("firebase-uid", "missing-course", "2026-1"))
                .thenThrow(new BusinessException(ErrorCode.COURSE_NOT_FOUND));

        mockMvc.perform(
                        delete("/v1/timetables/my/courses/missing-course")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .param("semester", "2026-1")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("COURSE_NOT_FOUND"));
    }

    @Test
    void deleteCourse_정상요청_200() throws Exception {
        mockValidToken();
        when(timetableService.removeCourse("firebase-uid", "course-1", "2026-1"))
                .thenReturn(new UserTimetableResponse(
                        "timetable-1",
                        "2026-1",
                        0,
                        0,
                        List.of(),
                        List.of()
                ));

        mockMvc.perform(
                        delete("/v1/timetables/my/courses/course-1")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .param("semester", "2026-1")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.semester").value("2026-1"))
                .andExpect(jsonPath("$.data.courseCount").value(0))
                .andExpect(jsonPath("$.data.courses").isArray());
    }

    @Test
    void deleteCourse_학기누락_400() throws Exception {
        mockValidToken();

        mockMvc.perform(
                        delete("/v1/timetables/my/courses/course-1")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
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

    private UserTimetableResponse timetableResponse() {
        return new UserTimetableResponse(
                "timetable-1",
                "2026-1",
                1,
                3,
                List.of(new TimetableCourseResponse(
                        "course-1",
                        "01255",
                        "001",
                        "민법총칙",
                        "문상혁",
                        "영401",
                        "전공선택",
                        3,
                        List.of(new CourseScheduleResponse(1, 3, 4))
                )),
                List.of(new TimetableSlotResponse(
                        "course-1",
                        "민법총칙",
                        "01255",
                        1,
                        3,
                        4,
                        "문상혁",
                        "영401"
                ))
        );
    }
}
