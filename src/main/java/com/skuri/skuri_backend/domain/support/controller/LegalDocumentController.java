package com.skuri.skuri_backend.domain.support.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.domain.support.dto.response.LegalDocumentResponse;
import com.skuri.skuri_backend.domain.support.service.LegalDocumentService;
import com.skuri.skuri_backend.infra.openapi.OpenApiCommonExamples;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/legal-documents")
@Tag(name = "Support Legal Document API", description = "이용약관/개인정보 처리방침 공개 조회 API")
public class LegalDocumentController {

    private final LegalDocumentService legalDocumentService;

    @GetMapping("/{documentKey}")
    @Operation(
            summary = "법적 문서 공개 조회",
            description = "로그인 전에도 호출 가능한 공개 API입니다. 활성화된 문서만 조회할 수 있습니다.",
            security = @SecurityRequirement(name = "")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiLegalSchemas.LegalDocumentApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiLegalExamples.SUCCESS_LEGAL_DOCUMENT_DETAIL)
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
                    responseCode = "404",
                    description = "문서 없음 또는 비활성",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "legal_document_not_found", value = OpenApiLegalExamples.ERROR_LEGAL_DOCUMENT_NOT_FOUND)
                    )
            )
    })
    public ResponseEntity<ApiResponse<LegalDocumentResponse>> getLegalDocument(
            @Parameter(description = "문서 키", example = "termsOfUse")
            @PathVariable String documentKey
    ) {
        return ResponseEntity.ok(ApiResponse.success(legalDocumentService.getLegalDocument(documentKey)));
    }
}
