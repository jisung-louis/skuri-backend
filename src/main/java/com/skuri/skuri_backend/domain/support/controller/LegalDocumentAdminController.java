package com.skuri.skuri_backend.domain.support.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.domain.support.dto.request.UpsertLegalDocumentRequest;
import com.skuri.skuri_backend.domain.support.dto.response.LegalDocumentAdminResponse;
import com.skuri.skuri_backend.domain.support.dto.response.LegalDocumentAdminSummaryResponse;
import com.skuri.skuri_backend.domain.support.dto.response.LegalDocumentDeleteResponse;
import com.skuri.skuri_backend.domain.support.service.LegalDocumentService;
import com.skuri.skuri_backend.infra.admin.audit.AdminAudit;
import com.skuri.skuri_backend.infra.admin.audit.AdminAuditActions;
import com.skuri.skuri_backend.infra.admin.audit.AdminAuditTargetTypes;
import com.skuri.skuri_backend.infra.auth.config.AdminApiAccess;
import com.skuri.skuri_backend.infra.openapi.OpenApiCommonExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiConfig;
import com.skuri.skuri_backend.infra.openapi.OpenApiLegalExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiLegalSchemas;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/admin/legal-documents")
@Tag(name = "Admin Legal Document API", description = "관리자 이용약관/개인정보 처리방침 관리 API")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@AdminApiAccess
public class LegalDocumentAdminController {

    private final LegalDocumentService legalDocumentService;

    @GetMapping
    @Operation(summary = "법적 문서 목록 조회(관리자)", description = "관리자가 법적 문서 목록 요약을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiLegalSchemas.LegalDocumentAdminSummaryListApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiLegalExamples.SUCCESS_ADMIN_LEGAL_DOCUMENT_SUMMARIES)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "unauthorized", value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)
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
            )
    })
    public ResponseEntity<ApiResponse<List<LegalDocumentAdminSummaryResponse>>> getLegalDocuments() {
        return ResponseEntity.ok(ApiResponse.success(legalDocumentService.getAdminLegalDocuments()));
    }

    @GetMapping("/{documentKey}")
    @Operation(summary = "법적 문서 상세 조회(관리자)", description = "관리자가 활성/비활성 여부와 무관하게 문서 상세를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiLegalSchemas.LegalDocumentAdminApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiLegalExamples.SUCCESS_ADMIN_LEGAL_DOCUMENT_DETAIL)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 documentKey",
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
                            examples = @ExampleObject(name = "unauthorized", value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)
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
                    description = "문서 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "legal_document_not_found", value = OpenApiLegalExamples.ERROR_LEGAL_DOCUMENT_NOT_FOUND)
                    )
            )
    })
    public ResponseEntity<ApiResponse<LegalDocumentAdminResponse>> getLegalDocument(
            @Parameter(description = "문서 키", example = "termsOfUse")
            @PathVariable String documentKey
    ) {
        return ResponseEntity.ok(ApiResponse.success(legalDocumentService.getAdminLegalDocument(documentKey)));
    }

    @PutMapping("/{documentKey}")
    @Operation(summary = "법적 문서 저장/수정(관리자)", description = "고정 문서 키 기준으로 생성 또는 전체 교체 저장합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "저장 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiLegalSchemas.LegalDocumentAdminApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiLegalExamples.SUCCESS_ADMIN_LEGAL_DOCUMENT_UPSERT)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 documentKey",
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
                            examples = @ExampleObject(name = "unauthorized", value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)
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
            description = "법적 문서 전체 저장 요청",
            content = @Content(
                    schema = @Schema(implementation = UpsertLegalDocumentRequest.class),
                    examples = @ExampleObject(value = OpenApiLegalExamples.REQUEST_ADMIN_LEGAL_DOCUMENT_UPSERT)
            )
    )
    @AdminAudit(
            action = AdminAuditActions.LEGAL_DOCUMENT_UPSERTED,
            targetType = AdminAuditTargetTypes.LEGAL_DOCUMENT,
            targetId = "@adminAuditTargetKeys.legalDocumentKey(#documentKey)",
            before = "@adminAuditSnapshots.legalDocument(@adminAuditTargetKeys.legalDocumentKey(#documentKey))",
            after = "@adminAuditSnapshots.legalDocument(@adminAuditTargetKeys.legalDocumentKey(#documentKey))"
    )
    public ResponseEntity<ApiResponse<LegalDocumentAdminResponse>> upsertLegalDocument(
            @Parameter(description = "문서 키", example = "termsOfUse")
            @PathVariable String documentKey,
            @Valid @RequestBody UpsertLegalDocumentRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(legalDocumentService.upsertLegalDocument(documentKey, request)));
    }

    @DeleteMapping("/{documentKey}")
    @Operation(summary = "법적 문서 삭제(관리자)", description = "관리자가 법적 문서를 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "삭제 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiLegalSchemas.LegalDocumentDeleteApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiLegalExamples.SUCCESS_ADMIN_LEGAL_DOCUMENT_DELETE)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 documentKey",
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
                            examples = @ExampleObject(name = "unauthorized", value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)
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
                    description = "문서 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "legal_document_not_found", value = OpenApiLegalExamples.ERROR_LEGAL_DOCUMENT_NOT_FOUND)
                    )
            )
    })
    @AdminAudit(
            action = AdminAuditActions.LEGAL_DOCUMENT_DELETED,
            targetType = AdminAuditTargetTypes.LEGAL_DOCUMENT,
            targetId = "@adminAuditTargetKeys.legalDocumentKey(#documentKey)",
            before = "@adminAuditSnapshots.legalDocument(@adminAuditTargetKeys.legalDocumentKey(#documentKey))"
    )
    public ResponseEntity<ApiResponse<LegalDocumentDeleteResponse>> deleteLegalDocument(
            @Parameter(description = "문서 키", example = "termsOfUse")
            @PathVariable String documentKey
    ) {
        return ResponseEntity.ok(ApiResponse.success(legalDocumentService.deleteLegalDocument(documentKey)));
    }
}
