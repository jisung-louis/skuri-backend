package com.skuri.skuri_backend.domain.academic.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.domain.academic.dto.request.CreateAcademicScheduleRequest;
import com.skuri.skuri_backend.domain.academic.dto.request.UpdateAcademicScheduleRequest;
import com.skuri.skuri_backend.domain.academic.dto.response.AcademicScheduleResponse;
import com.skuri.skuri_backend.domain.academic.service.AcademicScheduleService;
import com.skuri.skuri_backend.infra.admin.audit.AdminAudit;
import com.skuri.skuri_backend.infra.admin.audit.AdminAuditActions;
import com.skuri.skuri_backend.infra.admin.audit.AdminAuditTargetTypes;
import com.skuri.skuri_backend.infra.auth.config.AdminApiAccess;
import com.skuri.skuri_backend.infra.openapi.OpenApiAcademicExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiCommonExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/admin/academic-schedules")
@Tag(name = "Academic Schedule Admin API", description = "관리자 학사 일정 CRUD API")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@AdminApiAccess
public class AcademicScheduleAdminController {

    private final AcademicScheduleService academicScheduleService;

    @PostMapping
    @Operation(summary = "학사 일정 생성(관리자)", description = "관리자가 학사 일정을 생성합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiAcademicExamples.SUCCESS_ADMIN_ACADEMIC_SCHEDULE)
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
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "학사 일정 생성 요청",
            content = @Content(
                    schema = @Schema(implementation = CreateAcademicScheduleRequest.class),
                    examples = @ExampleObject(value = """
                            {
                              "title": "중간고사",
                              "startDate": "2026-04-15",
                              "endDate": "2026-04-21",
                              "type": "MULTI",
                              "isPrimary": true,
                              "description": "2026학년도 1학기 중간고사"
                            }
                            """)
            )
    )
    @AdminAudit(
            action = AdminAuditActions.ACADEMIC_SCHEDULE_CREATED,
            targetType = AdminAuditTargetTypes.ACADEMIC_SCHEDULE,
            targetId = "#responseBody['data']['id']",
            after = "@adminAuditSnapshots.academicSchedule(#responseBody['data']['id'])"
    )
    public ResponseEntity<ApiResponse<AcademicScheduleResponse>> createAcademicSchedule(
            @Valid @RequestBody CreateAcademicScheduleRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(academicScheduleService.createSchedule(request)));
    }

    @PutMapping("/{scheduleId}")
    @Operation(summary = "학사 일정 수정(관리자)", description = "관리자가 학사 일정을 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiAcademicExamples.SUCCESS_ADMIN_ACADEMIC_SCHEDULE)
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
                    responseCode = "404",
                    description = "학사 일정 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "schedule_not_found", value = OpenApiAcademicExamples.ERROR_ACADEMIC_SCHEDULE_NOT_FOUND)
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
            action = AdminAuditActions.ACADEMIC_SCHEDULE_UPDATED,
            targetType = AdminAuditTargetTypes.ACADEMIC_SCHEDULE,
            targetId = "#scheduleId",
            before = "@adminAuditSnapshots.academicSchedule(#scheduleId)",
            after = "@adminAuditSnapshots.academicSchedule(#scheduleId)"
    )
    public ResponseEntity<ApiResponse<AcademicScheduleResponse>> updateAcademicSchedule(
            @Parameter(description = "학사 일정 ID", example = "schedule_uuid")
            @PathVariable String scheduleId,
            @Valid @RequestBody UpdateAcademicScheduleRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(academicScheduleService.updateSchedule(scheduleId, request)));
    }

    @DeleteMapping("/{scheduleId}")
    @Operation(summary = "학사 일정 삭제(관리자)", description = "관리자가 학사 일정을 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "삭제 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.SUCCESS_NULL)
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
                    responseCode = "404",
                    description = "학사 일정 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "schedule_not_found", value = OpenApiAcademicExamples.ERROR_ACADEMIC_SCHEDULE_NOT_FOUND)
                    )
            )
    })
    @AdminAudit(
            action = AdminAuditActions.ACADEMIC_SCHEDULE_DELETED,
            targetType = AdminAuditTargetTypes.ACADEMIC_SCHEDULE,
            targetId = "#scheduleId",
            before = "@adminAuditSnapshots.academicSchedule(#scheduleId)"
    )
    public ResponseEntity<ApiResponse<Void>> deleteAcademicSchedule(
            @Parameter(description = "학사 일정 ID", example = "schedule_uuid")
            @PathVariable String scheduleId
    ) {
        academicScheduleService.deleteSchedule(scheduleId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
