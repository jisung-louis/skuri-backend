package com.skuri.skuri_backend.domain.support.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.domain.support.dto.request.CreateCafeteriaMenuRequest;
import com.skuri.skuri_backend.domain.support.dto.request.UpdateCafeteriaMenuRequest;
import com.skuri.skuri_backend.domain.support.dto.response.CafeteriaMenuResponse;
import com.skuri.skuri_backend.domain.support.service.CafeteriaMenuService;
import com.skuri.skuri_backend.infra.admin.audit.AdminAudit;
import com.skuri.skuri_backend.infra.admin.audit.AdminAuditActions;
import com.skuri.skuri_backend.infra.admin.audit.AdminAuditTargetTypes;
import com.skuri.skuri_backend.infra.auth.config.AdminApiAccess;
import com.skuri.skuri_backend.infra.openapi.OpenApiCommonExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiConfig;
import com.skuri.skuri_backend.infra.openapi.OpenApiSupportExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiSupportSchemas;
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
@RequestMapping("/v1/admin/cafeteria-menus")
@Tag(name = "Admin Support Cafeteria API", description = "관리자 학식 메뉴 관리 API")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@AdminApiAccess
public class CafeteriaMenuAdminController {

    private final CafeteriaMenuService cafeteriaMenuService;

    @PostMapping
    @Operation(summary = "학식 메뉴 등록(관리자)", description = "주차 단위 학식 메뉴를 등록합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "등록 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiSupportSchemas.CafeteriaMenuApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiSupportExamples.SUCCESS_ADMIN_CAFETERIA_MENU_CREATE)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "weekId/기간 불일치 또는 menus/menuEntries 불일치",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "invalid_request", value = OpenApiCommonExamples.ERROR_INVALID_REQUEST),
                                    @ExampleObject(
                                            name = "menus_menu_entries_mismatch",
                                            value = OpenApiSupportExamples.ERROR_CAFETERIA_MENU_MENU_ENTRIES_MISMATCH
                                    )
                            }
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
                    description = "이미 등록된 주차",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "cafeteria_menu_already_exists", value = OpenApiSupportExamples.ERROR_CAFETERIA_MENU_ALREADY_EXISTS)
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
            description = "학식 메뉴 등록 요청",
            content = @Content(
                    schema = @Schema(implementation = CreateCafeteriaMenuRequest.class),
                    examples = {
                            @ExampleObject(
                                    name = "structured_menu_entries",
                                    summary = "프론트용 구조화 메타데이터만 등록",
                                    value = """
                                            {
                                              "weekId": "2026-W08",
                                              "weekStart": "2026-02-16",
                                              "weekEnd": "2026-02-20",
                                              "menuEntries": {
                                                "2026-02-16": {
                                                  "rollNoodles": [
                                                    {
                                                      "title": "존슨부대찌개",
                                                      "badges": [
                                                        {
                                                          "code": "TAKEOUT",
                                                          "label": "테이크아웃"
                                                        }
                                                      ],
                                                      "likeCount": 178,
                                                      "dislikeCount": 22
                                                    }
                                                  ],
                                                  "theBab": [
                                                    {
                                                      "title": "돈까스",
                                                      "badges": [],
                                                      "likeCount": 31,
                                                      "dislikeCount": 4
                                                    }
                                                  ],
                                                  "fryRice": []
                                                }
                                              }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "legacy_menus",
                                    summary = "기존 menus 기반 등록",
                                    value = """
                                            {
                                              "weekId": "2026-W08",
                                              "weekStart": "2026-02-16",
                                              "weekEnd": "2026-02-20",
                                              "menus": {
                                                "2026-02-16": {
                                                  "rollNoodles": ["우동", "김밥"],
                                                  "theBab": ["돈까스", "된장찌개"],
                                                  "fryRice": ["볶음밥", "짜장면"]
                                                }
                                              }
                                            }
                                            """
                            )
                    }
            )
    )
    @AdminAudit(
            action = AdminAuditActions.CAFETERIA_MENU_CREATED,
            targetType = AdminAuditTargetTypes.CAFETERIA_MENU,
            targetId = "#requestBody['weekId']",
            before = "@adminAuditSnapshots.cafeteriaMenu(#requestBody['weekId'])",
            after = "@adminAuditSnapshots.cafeteriaMenu(#requestBody['weekId'])"
    )
    public ResponseEntity<ApiResponse<CafeteriaMenuResponse>> createMenu(
            @Valid @RequestBody CreateCafeteriaMenuRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(cafeteriaMenuService.createMenu(request)));
    }

    @PutMapping("/{weekId}")
    @Operation(summary = "학식 메뉴 수정(관리자)", description = "기존 주차의 학식 메뉴를 전체 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiSupportSchemas.CafeteriaMenuApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiSupportExamples.SUCCESS_ADMIN_CAFETERIA_MENU_UPDATE)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "weekId/기간 불일치 또는 menus/menuEntries 불일치",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "invalid_request", value = OpenApiCommonExamples.ERROR_INVALID_REQUEST),
                                    @ExampleObject(
                                            name = "menus_menu_entries_mismatch",
                                            value = OpenApiSupportExamples.ERROR_CAFETERIA_MENU_MENU_ENTRIES_MISMATCH
                                    )
                            }
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
                    description = "학식 메뉴 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "cafeteria_menu_not_found", value = OpenApiSupportExamples.ERROR_CAFETERIA_MENU_NOT_FOUND)
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
            description = "학식 메뉴 수정 요청",
            content = @Content(
                    schema = @Schema(implementation = UpdateCafeteriaMenuRequest.class),
                    examples = {
                            @ExampleObject(
                                    name = "structured_menu_entries",
                                    summary = "프론트용 구조화 메타데이터만 수정",
                                    value = """
                                            {
                                              "weekStart": "2026-02-16",
                                              "weekEnd": "2026-02-20",
                                              "menuEntries": {
                                                "2026-02-16": {
                                                  "rollNoodles": [
                                                    {
                                                      "title": "존슨부대찌개",
                                                      "badges": [
                                                        {
                                                          "code": "TAKEOUT",
                                                          "label": "테이크아웃"
                                                        }
                                                      ],
                                                      "likeCount": 178,
                                                      "dislikeCount": 22
                                                    }
                                                  ],
                                                  "theBab": [
                                                    {
                                                      "title": "돈까스",
                                                      "badges": [],
                                                      "likeCount": 31,
                                                      "dislikeCount": 4
                                                    }
                                                  ],
                                                  "fryRice": []
                                                }
                                              }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "legacy_menus",
                                    summary = "기존 menus 기반 수정",
                                    value = """
                                            {
                                              "weekStart": "2026-02-16",
                                              "weekEnd": "2026-02-20",
                                              "menus": {
                                                "2026-02-16": {
                                                  "rollNoodles": ["우동", "김밥"],
                                                  "theBab": ["돈까스", "된장찌개"],
                                                  "fryRice": ["볶음밥", "짜장면"]
                                                }
                                              }
                                            }
                                            """
                            )
                    }
            )
    )
    @AdminAudit(
            action = AdminAuditActions.CAFETERIA_MENU_UPDATED,
            targetType = AdminAuditTargetTypes.CAFETERIA_MENU,
            targetId = "#weekId",
            before = "@adminAuditSnapshots.cafeteriaMenu(#weekId)",
            after = "@adminAuditSnapshots.cafeteriaMenu(#weekId)"
    )
    public ResponseEntity<ApiResponse<CafeteriaMenuResponse>> updateMenu(
            @Parameter(description = "주차 ID", example = "2026-W08")
            @PathVariable String weekId,
            @Valid @RequestBody UpdateCafeteriaMenuRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(cafeteriaMenuService.updateMenu(weekId, request)));
    }

    @DeleteMapping("/{weekId}")
    @Operation(summary = "학식 메뉴 삭제(관리자)", description = "주차 단위 학식 메뉴를 삭제합니다.")
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
                    responseCode = "400",
                    description = "잘못된 weekId",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "invalid_request", value = OpenApiCommonExamples.ERROR_INVALID_REQUEST)
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
                    description = "학식 메뉴 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "cafeteria_menu_not_found", value = OpenApiSupportExamples.ERROR_CAFETERIA_MENU_NOT_FOUND)
                    )
            )
    })
    @AdminAudit(
            action = AdminAuditActions.CAFETERIA_MENU_DELETED,
            targetType = AdminAuditTargetTypes.CAFETERIA_MENU,
            targetId = "#weekId",
            before = "@adminAuditSnapshots.cafeteriaMenu(#weekId)"
    )
    public ResponseEntity<ApiResponse<Void>> deleteMenu(
            @Parameter(description = "주차 ID", example = "2026-W08")
            @PathVariable String weekId
    ) {
        cafeteriaMenuService.deleteMenu(weekId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
