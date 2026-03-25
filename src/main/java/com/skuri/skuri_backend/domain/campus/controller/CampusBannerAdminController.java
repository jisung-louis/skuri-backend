package com.skuri.skuri_backend.domain.campus.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.domain.campus.dto.request.CreateCampusBannerRequest;
import com.skuri.skuri_backend.domain.campus.dto.request.ReorderCampusBannersRequest;
import com.skuri.skuri_backend.domain.campus.dto.request.UpdateCampusBannerRequest;
import com.skuri.skuri_backend.domain.campus.dto.response.CampusBannerAdminResponse;
import com.skuri.skuri_backend.domain.campus.dto.response.CampusBannerOrderResponse;
import com.skuri.skuri_backend.domain.campus.service.CampusBannerService;
import com.skuri.skuri_backend.infra.admin.audit.AdminAudit;
import com.skuri.skuri_backend.infra.admin.audit.AdminAuditActions;
import com.skuri.skuri_backend.infra.admin.audit.AdminAuditTargetTypes;
import com.skuri.skuri_backend.infra.auth.config.AdminApiAccess;
import com.skuri.skuri_backend.infra.openapi.OpenApiCampusExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiCampusSchemas;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/admin/campus-banners")
@Tag(name = "Campus Banner Admin API", description = "관리자 캠퍼스 홈 배너 관리 API")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@AdminApiAccess
public class CampusBannerAdminController {

    private final CampusBannerService campusBannerService;

    @GetMapping
    @Operation(summary = "캠퍼스 홈 배너 목록 조회(관리자)", description = "관리자가 전체 배너를 표시 순서대로 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiCampusSchemas.CampusBannerAdminListApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCampusExamples.SUCCESS_ADMIN_CAMPUS_BANNERS_LIST)
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
            )
    })
    public ResponseEntity<ApiResponse<List<CampusBannerAdminResponse>>> getCampusBanners() {
        return ResponseEntity.ok(ApiResponse.success(campusBannerService.getAdminBanners()));
    }

    @GetMapping("/{bannerId}")
    @Operation(summary = "캠퍼스 홈 배너 상세 조회(관리자)", description = "관리자가 단일 배너 상세를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiCampusSchemas.CampusBannerAdminApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCampusExamples.SUCCESS_ADMIN_CAMPUS_BANNER_DETAIL)
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
                    description = "캠퍼스 배너 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "campus_banner_not_found", value = OpenApiCampusExamples.ERROR_CAMPUS_BANNER_NOT_FOUND)
                    )
            )
    })
    public ResponseEntity<ApiResponse<CampusBannerAdminResponse>> getCampusBanner(
            @Parameter(description = "캠퍼스 배너 ID", example = "campus_banner_uuid_1")
            @PathVariable String bannerId
    ) {
        return ResponseEntity.ok(ApiResponse.success(campusBannerService.getAdminBanner(bannerId)));
    }

    @PostMapping
    @Operation(summary = "캠퍼스 홈 배너 생성(관리자)", description = "관리자가 캠퍼스 홈 배너를 생성합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiCampusSchemas.CampusBannerAdminApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCampusExamples.SUCCESS_ADMIN_CAMPUS_BANNER_CREATE)
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
            description = "캠퍼스 홈 배너 생성 요청",
            content = @Content(
                    schema = @Schema(implementation = CreateCampusBannerRequest.class),
                    examples = @ExampleObject(name = "default", value = OpenApiCampusExamples.REQUEST_ADMIN_CAMPUS_BANNER_CREATE)
            )
    )
    @AdminAudit(
            action = AdminAuditActions.CAMPUS_BANNER_CREATED,
            targetType = AdminAuditTargetTypes.CAMPUS_BANNER,
            targetId = "#responseBody['data']['id']",
            after = "@adminAuditSnapshots.campusBanner(#responseBody['data']['id'])"
    )
    public ResponseEntity<ApiResponse<CampusBannerAdminResponse>> createCampusBanner(
            @Valid @RequestBody CreateCampusBannerRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(campusBannerService.createBanner(request)));
    }

    @PatchMapping("/{bannerId}")
    @Operation(summary = "캠퍼스 홈 배너 부분 수정(관리자)", description = "전달한 필드만 반영하며, `null`도 명시적 값으로 처리합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiCampusSchemas.CampusBannerAdminApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCampusExamples.SUCCESS_ADMIN_CAMPUS_BANNER_UPDATE)
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
                    description = "캠퍼스 배너 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "campus_banner_not_found", value = OpenApiCampusExamples.ERROR_CAMPUS_BANNER_NOT_FOUND)
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
            description = "캠퍼스 홈 배너 부분 수정 요청",
            content = @Content(
                    schema = @Schema(implementation = UpdateCampusBannerRequest.class),
                    examples = @ExampleObject(name = "default", value = OpenApiCampusExamples.REQUEST_ADMIN_CAMPUS_BANNER_UPDATE)
            )
    )
    @AdminAudit(
            action = AdminAuditActions.CAMPUS_BANNER_UPDATED,
            targetType = AdminAuditTargetTypes.CAMPUS_BANNER,
            targetId = "#bannerId",
            before = "@adminAuditSnapshots.campusBanner(#bannerId)",
            after = "@adminAuditSnapshots.campusBanner(#bannerId)"
    )
    public ResponseEntity<ApiResponse<CampusBannerAdminResponse>> updateCampusBanner(
            @Parameter(description = "캠퍼스 배너 ID", example = "campus_banner_uuid_1")
            @PathVariable String bannerId,
            @Valid @RequestBody UpdateCampusBannerRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(campusBannerService.updateBanner(bannerId, request)));
    }

    @DeleteMapping("/{bannerId}")
    @Operation(summary = "캠퍼스 홈 배너 삭제(관리자)", description = "관리자가 캠퍼스 홈 배너를 삭제합니다.")
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
                    description = "캠퍼스 배너 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "campus_banner_not_found", value = OpenApiCampusExamples.ERROR_CAMPUS_BANNER_NOT_FOUND)
                    )
            )
    })
    @AdminAudit(
            action = AdminAuditActions.CAMPUS_BANNER_DELETED,
            targetType = AdminAuditTargetTypes.CAMPUS_BANNER,
            targetId = "#bannerId",
            before = "@adminAuditSnapshots.campusBanner(#bannerId)"
    )
    public ResponseEntity<ApiResponse<Void>> deleteCampusBanner(
            @Parameter(description = "캠퍼스 배너 ID", example = "campus_banner_uuid_1")
            @PathVariable String bannerId
    ) {
        campusBannerService.deleteBanner(bannerId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/order")
    @Operation(summary = "캠퍼스 홈 배너 순서 변경(관리자)", description = "전체 배너 ID 목록을 전달받아 표시 순서를 재정렬합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "순서 변경 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiCampusSchemas.CampusBannerOrderListApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCampusExamples.SUCCESS_ADMIN_CAMPUS_BANNER_REORDER)
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
                    description = "캠퍼스 배너 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "campus_banner_not_found", value = OpenApiCampusExamples.ERROR_CAMPUS_BANNER_NOT_FOUND)
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
            description = "캠퍼스 홈 배너 순서 변경 요청",
            content = @Content(
                    schema = @Schema(implementation = ReorderCampusBannersRequest.class),
                    examples = @ExampleObject(name = "default", value = OpenApiCampusExamples.REQUEST_ADMIN_CAMPUS_BANNER_REORDER)
            )
    )
    @AdminAudit(
            action = AdminAuditActions.CAMPUS_BANNER_REORDERED,
            targetType = AdminAuditTargetTypes.CAMPUS_BANNER,
            targetId = "'display-order'",
            before = "@adminAuditSnapshots.campusBannerOrder()",
            after = "@adminAuditSnapshots.campusBannerOrder()"
    )
    public ResponseEntity<ApiResponse<List<CampusBannerOrderResponse>>> reorderCampusBanners(
            @Valid @RequestBody ReorderCampusBannersRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(campusBannerService.reorderBanners(request)));
    }
}
