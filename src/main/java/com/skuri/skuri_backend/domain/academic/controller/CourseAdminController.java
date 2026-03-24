package com.skuri.skuri_backend.domain.academic.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.domain.academic.dto.request.AdminBulkCoursesRequest;
import com.skuri.skuri_backend.domain.academic.dto.response.AdminBulkCoursesResponse;
import com.skuri.skuri_backend.domain.academic.service.CourseService;
import com.skuri.skuri_backend.infra.admin.audit.AdminAudit;
import com.skuri.skuri_backend.infra.admin.audit.AdminAuditActions;
import com.skuri.skuri_backend.infra.admin.audit.AdminAuditTargetTypes;
import com.skuri.skuri_backend.infra.auth.config.AdminApiAccess;
import com.skuri.skuri_backend.infra.openapi.OpenApiAcademicExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiAcademicSchemas;
import com.skuri.skuri_backend.infra.openapi.OpenApiCommonExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/admin/courses")
@Tag(name = "Academic Course Admin API", description = "관리자 강의 일괄 등록/삭제 API")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@AdminApiAccess
public class CourseAdminController {

    private final CourseService courseService;

    @PostMapping("/bulk")
    @Operation(summary = "학기 강의 일괄 등록(관리자)", description = "semester/code/division 기준으로 강의를 업서트하고 누락 강의는 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "처리 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiAcademicSchemas.AdminBulkCoursesApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiAcademicExamples.SUCCESS_ADMIN_COURSE_BULK)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "관리자 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "admin_required", value = OpenApiCommonExamples.ERROR_ADMIN_REQUIRED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "강의 bulk 처리 중 충돌 발생",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "bulk_conflict", value = OpenApiAcademicExamples.ERROR_ADMIN_COURSE_BULK_CONFLICT)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "요청 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "validation_error", value = OpenApiCommonExamples.ERROR_VALIDATION)
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "학기 강의 일괄 등록 요청",
            content = @Content(
                    schema = @Schema(implementation = AdminBulkCoursesRequest.class),
                    examples = @ExampleObject(value = """
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
                                    { "dayOfWeek": 1, "startPeriod": 3, "endPeriod": 4 },
                                    { "dayOfWeek": 3, "startPeriod": 3, "endPeriod": 4 }
                                  ]
                                }
                              ]
                            }
                            """)
            )
    )
    @AdminAudit(
            action = AdminAuditActions.COURSE_SEMESTER_BULK_UPSERTED,
            targetType = AdminAuditTargetTypes.COURSE_SEMESTER,
            targetId = "@adminAuditTargetKeys.courseSemester(#requestBody['semester'])",
            before = "@adminAuditSnapshots.courseSemester(@adminAuditTargetKeys.courseSemester(#requestBody['semester']))",
            after = "@adminAuditSnapshots.courseSemester(@adminAuditTargetKeys.courseSemester(#requestBody['semester']))"
    )
    public ResponseEntity<ApiResponse<AdminBulkCoursesResponse>> bulkUpsertCourses(
            @Valid @RequestBody AdminBulkCoursesRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(courseService.bulkUpsertCourses(request)));
    }

    @DeleteMapping
    @Operation(summary = "학기 강의 전체 삭제(관리자)", description = "특정 학기의 강의를 모두 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "필수 요청 파라미터 누락 또는 형식 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "invalid_request", value = OpenApiCommonExamples.ERROR_INVALID_REQUEST)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "삭제 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiAcademicSchemas.AdminBulkCoursesApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiAcademicExamples.SUCCESS_ADMIN_COURSE_DELETE)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "관리자 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "admin_required", value = OpenApiCommonExamples.ERROR_ADMIN_REQUIRED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "요청 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "validation_error", value = OpenApiCommonExamples.ERROR_VALIDATION)
                    )
            )
    })
    @AdminAudit(
            action = AdminAuditActions.COURSE_SEMESTER_DELETED,
            targetType = AdminAuditTargetTypes.COURSE_SEMESTER,
            targetId = "@adminAuditTargetKeys.courseSemester(#semester)",
            before = "@adminAuditSnapshots.courseSemester(@adminAuditTargetKeys.courseSemester(#semester))"
    )
    public ResponseEntity<ApiResponse<AdminBulkCoursesResponse>> deleteCourses(
            @io.swagger.v3.oas.annotations.Parameter(description = "삭제 대상 학기", example = "2026-1", required = true)
            @RequestParam(name = "semester") String semester
    ) {
        return ResponseEntity.ok(ApiResponse.success(courseService.deleteSemesterCourses(semester)));
    }
}
