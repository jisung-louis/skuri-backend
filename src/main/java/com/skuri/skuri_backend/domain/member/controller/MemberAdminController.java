package com.skuri.skuri_backend.domain.member.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.common.dto.PageResponse;
import com.skuri.skuri_backend.domain.member.dto.request.UpdateMemberAdminRoleRequest;
import com.skuri.skuri_backend.domain.member.dto.response.AdminMemberDetailResponse;
import com.skuri.skuri_backend.domain.member.dto.response.AdminMemberSummaryResponse;
import com.skuri.skuri_backend.domain.member.entity.MemberStatus;
import com.skuri.skuri_backend.domain.member.service.MemberAdminService;
import com.skuri.skuri_backend.infra.admin.audit.AdminAudit;
import com.skuri.skuri_backend.infra.admin.audit.AdminAuditActions;
import com.skuri.skuri_backend.infra.admin.audit.AdminAuditTargetTypes;
import com.skuri.skuri_backend.infra.auth.config.AdminApiAccess;
import com.skuri.skuri_backend.infra.openapi.OpenApiCommonExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiConfig;
import com.skuri.skuri_backend.infra.openapi.OpenApiMemberExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiMemberSchemas;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/admin/members")
@Tag(name = "Admin Member API", description = "관리자 회원 관리 API")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@AdminApiAccess
public class MemberAdminController {

    private final MemberAdminService memberAdminService;

    @GetMapping
    @Operation(summary = "회원 목록 조회(관리자)", description = "검색/필터/페이지네이션으로 회원 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiMemberSchemas.AdminMemberPageApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiMemberExamples.SUCCESS_ADMIN_MEMBERS_PAGE)
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
                    description = "잘못된 필터 값",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "invalid_request", value = OpenApiCommonExamples.ERROR_INVALID_REQUEST)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "페이지네이션/필터 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "validation_error", value = OpenApiCommonExamples.ERROR_VALIDATION)
                    )
            )
    })
    public ResponseEntity<ApiResponse<PageResponse<AdminMemberSummaryResponse>>> getAdminMembers(
            @Parameter(description = "검색어(email/nickname/realname/studentId 부분 검색)", example = "홍길동")
            @RequestParam(name = "query", required = false) String query,
            @Parameter(description = "회원 상태 필터", example = "ACTIVE")
            @RequestParam(name = "status", required = false) MemberStatus status,
            @Parameter(description = "관리자 여부 필터", example = "true")
            @RequestParam(name = "isAdmin", required = false) Boolean isAdmin,
            @Parameter(description = "학과 필터", example = "컴퓨터공학과")
            @RequestParam(name = "department", required = false) String department,
            @Parameter(description = "페이지 번호", example = "0")
            @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                memberAdminService.getAdminMembers(query, status, isAdmin, department, page, size)
        ));
    }

    @GetMapping("/{memberId}")
    @Operation(summary = "회원 상세 조회(관리자)", description = "운영에 필요한 회원 상세 정보를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiMemberSchemas.AdminMemberDetailApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiMemberExamples.SUCCESS_ADMIN_MEMBER_DETAIL)
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
                    description = "회원 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "member_not_found", value = OpenApiMemberExamples.ERROR_MEMBER_NOT_FOUND)
                    )
            )
    })
    public ResponseEntity<ApiResponse<AdminMemberDetailResponse>> getAdminMember(
            @Parameter(description = "조회할 회원 ID(Firebase UID)", example = "dw9rPtuticbjnaYPkeiF3RGPpqk1")
            @PathVariable String memberId
    ) {
        return ResponseEntity.ok(ApiResponse.success(memberAdminService.getAdminMember(memberId)));
    }

    @PatchMapping("/{memberId}/admin-role")
    @Operation(summary = "관리자 권한 변경(관리자)", description = "회원의 관리자 권한을 부여하거나 회수합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "변경 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiMemberSchemas.AdminMemberDetailApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiMemberExamples.SUCCESS_ADMIN_MEMBER_ADMIN_ROLE_UPDATED)
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
                    description = "회원 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "member_not_found", value = OpenApiMemberExamples.ERROR_MEMBER_NOT_FOUND)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "허용되지 않는 상태의 회원",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "withdrawn_member_admin_role_change_not_allowed",
                                    value = OpenApiMemberExamples.ERROR_MEMBER_ADMIN_ROLE_CHANGE_NOT_ALLOWED
                            )
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
            description = "관리자 권한 변경 요청",
            content = @Content(
                    schema = @Schema(implementation = UpdateMemberAdminRoleRequest.class),
                    examples = {
                            @ExampleObject(name = "grant_admin", value = "{\"isAdmin\": true}"),
                            @ExampleObject(name = "revoke_admin", value = "{\"isAdmin\": false}")
                    }
            )
    )
    @AdminAudit(
            action = AdminAuditActions.MEMBER_ADMIN_ROLE_UPDATED,
            targetType = AdminAuditTargetTypes.MEMBER,
            targetId = "#memberId",
            before = "@adminAuditSnapshots.member(#memberId)",
            after = "@adminAuditSnapshots.member(#memberId)"
    )
    public ResponseEntity<ApiResponse<AdminMemberDetailResponse>> updateAdminRole(
            @Parameter(description = "권한을 변경할 회원 ID(Firebase UID)", example = "dw9rPtuticbjnaYPkeiF3RGPpqk1")
            @PathVariable String memberId,
            @Valid @RequestBody UpdateMemberAdminRoleRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(memberAdminService.updateAdminRole(memberId, request)));
    }
}
