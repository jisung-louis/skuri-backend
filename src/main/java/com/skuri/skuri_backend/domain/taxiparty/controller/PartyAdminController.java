package com.skuri.skuri_backend.domain.taxiparty.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.common.dto.PageResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.request.UpdateAdminPartyStatusRequest;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.AdminPartyDetailResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.AdminPartySummaryResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyStatusResponse;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus;
import com.skuri.skuri_backend.domain.taxiparty.service.TaxiPartyAdminService;
import com.skuri.skuri_backend.infra.admin.audit.AdminAudit;
import com.skuri.skuri_backend.infra.admin.audit.AdminAuditActions;
import com.skuri.skuri_backend.infra.admin.audit.AdminAuditTargetTypes;
import com.skuri.skuri_backend.infra.auth.config.AdminApiAccess;
import com.skuri.skuri_backend.infra.openapi.OpenApiCommonExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiConfig;
import com.skuri.skuri_backend.infra.openapi.OpenApiTaxiPartyExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiTaxiPartySchemas;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/admin/parties")
@Tag(name = "TaxiParty Admin API", description = "관리자 택시 파티 운영 API")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@AdminApiAccess
public class PartyAdminController {

    private final TaxiPartyAdminService taxiPartyAdminService;

    @GetMapping
    @Operation(
            summary = "파티 목록 조회(관리자)",
            description = "관리자용 파티 목록을 검색/필터/페이지네이션으로 조회합니다. 기본 정렬은 departureTime DESC입니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiTaxiPartySchemas.AdminPartySummaryPageApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiTaxiPartyExamples.SUCCESS_ADMIN_PARTY_LIST_PAGE)
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
                    responseCode = "400",
                    description = "잘못된 enum 필터 값",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "invalid_request", value = OpenApiCommonExamples.ERROR_INVALID_REQUEST)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "페이지네이션 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "validation_error", value = OpenApiCommonExamples.ERROR_VALIDATION)
                    )
            )
    })
    public ResponseEntity<ApiResponse<PageResponse<AdminPartySummaryResponse>>> getAdminParties(
            @Parameter(description = "파티 상태 필터", example = "OPEN")
            @RequestParam(name = "status", required = false) PartyStatus status,
            @Parameter(description = "출발일 필터(yyyy-MM-dd)", example = "2026-03-29")
            @RequestParam(name = "departureDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate departureDate,
            @Parameter(description = "출발지/목적지/leader uid/leader nickname 검색어", example = "안양역")
            @RequestParam(name = "query", required = false) String query,
            @Parameter(description = "페이지 번호", example = "0")
            @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                taxiPartyAdminService.getAdminParties(status, departureDate, query, page, size)
        ));
    }

    @GetMapping("/{partyId}")
    @Operation(summary = "파티 상세 조회(관리자)", description = "관리자용 파티 상세와 운영 메타데이터를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiTaxiPartySchemas.AdminPartyDetailApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiTaxiPartyExamples.SUCCESS_ADMIN_PARTY_DETAIL)
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
                    description = "파티 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "party_not_found", value = OpenApiTaxiPartyExamples.ERROR_PARTY_NOT_FOUND)
                    )
            )
    })
    public ResponseEntity<ApiResponse<AdminPartyDetailResponse>> getAdminParty(
            @Parameter(description = "파티 ID", example = "party-20260304-001")
            @PathVariable String partyId
    ) {
        return ResponseEntity.ok(ApiResponse.success(taxiPartyAdminService.getAdminParty(partyId)));
    }

    @PatchMapping("/{partyId}/status")
    @Operation(
            summary = "파티 상태 변경(관리자)",
            description = "관리자가 기존 파티 상태 머신의 허용 전이만 재사용해 파티 상태를 강제 변경합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "변경 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiTaxiPartySchemas.PartyStatusApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "close", value = OpenApiTaxiPartyExamples.SUCCESS_PARTY_STATUS_CLOSED),
                                    @ExampleObject(name = "reopen", value = OpenApiTaxiPartyExamples.SUCCESS_PARTY_STATUS_OPEN),
                                    @ExampleObject(name = "cancel", value = OpenApiTaxiPartyExamples.SUCCESS_PARTY_STATUS_ENDED_CANCELLED),
                                    @ExampleObject(name = "end", value = OpenApiTaxiPartyExamples.SUCCESS_PARTY_STATUS_ENDED_FORCE)
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
                    description = "파티 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "party_not_found", value = OpenApiTaxiPartyExamples.ERROR_PARTY_NOT_FOUND)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "허용되지 않은 상태 전이/동시성 충돌",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "close_only", value = OpenApiTaxiPartyExamples.ERROR_INVALID_PARTY_STATE_TRANSITION_CLOSE_ONLY),
                                    @ExampleObject(name = "reopen_only", value = OpenApiTaxiPartyExamples.ERROR_INVALID_PARTY_STATE_TRANSITION_REOPEN_ONLY),
                                    @ExampleObject(name = "end_only", value = OpenApiTaxiPartyExamples.ERROR_INVALID_PARTY_STATE_TRANSITION_FORCE_END_ONLY),
                                    @ExampleObject(name = "party_not_cancelable", value = OpenApiTaxiPartyExamples.ERROR_PARTY_NOT_CANCELABLE),
                                    @ExampleObject(name = "party_ended", value = OpenApiTaxiPartyExamples.ERROR_PARTY_ENDED),
                                    @ExampleObject(name = "party_concurrent_modification", value = OpenApiTaxiPartyExamples.ERROR_PARTY_CONCURRENT_MODIFICATION)
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 enum 액션 값",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "invalid_request", value = OpenApiCommonExamples.ERROR_INVALID_REQUEST)
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
            description = "관리자 파티 상태 변경 요청",
            content = @Content(
                    schema = @Schema(implementation = UpdateAdminPartyStatusRequest.class),
                    examples = {
                            @ExampleObject(name = "close", value = "{\"action\":\"CLOSE\"}"),
                            @ExampleObject(name = "reopen", value = "{\"action\":\"REOPEN\"}"),
                            @ExampleObject(name = "cancel", value = "{\"action\":\"CANCEL\"}"),
                            @ExampleObject(name = "end", value = "{\"action\":\"END\"}")
                    }
            )
    )
    @AdminAudit(
            action = AdminAuditActions.PARTY_STATUS_UPDATED,
            targetType = AdminAuditTargetTypes.PARTY,
            targetId = "#partyId",
            before = "@adminAuditSnapshots.partyStatus(#partyId)",
            after = "@adminAuditSnapshots.partyStatus(#partyId)"
    )
    public ResponseEntity<ApiResponse<PartyStatusResponse>> updatePartyStatus(
            @Parameter(description = "파티 ID", example = "party-20260304-001")
            @PathVariable String partyId,
            @Valid @RequestBody UpdateAdminPartyStatusRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                taxiPartyAdminService.updatePartyStatus(partyId, request.action())
        ));
    }
}
