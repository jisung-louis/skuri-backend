package com.skuri.skuri_backend.domain.admin.dashboard.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.domain.admin.dashboard.dto.response.AdminDashboardActivityResponse;
import com.skuri.skuri_backend.domain.admin.dashboard.dto.response.AdminDashboardRecentItemResponse;
import com.skuri.skuri_backend.domain.admin.dashboard.dto.response.AdminDashboardSummaryResponse;
import com.skuri.skuri_backend.domain.admin.dashboard.service.AdminDashboardService;
import com.skuri.skuri_backend.infra.auth.config.AdminApiAccess;
import com.skuri.skuri_backend.infra.openapi.OpenApiCommonExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiConfig;
import com.skuri.skuri_backend.infra.openapi.OpenApiDashboardExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiDashboardSchemas;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/admin/dashboard")
@Tag(name = "Admin Dashboard API", description = "관리자 대시보드 read-model API")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@AdminApiAccess
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/summary")
    @Operation(
            summary = "대시보드 KPI 요약 조회(관리자)",
            description = "회원/파티/문의/신고 도메인의 핵심 KPI 카드를 한 번에 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiDashboardSchemas.AdminDashboardSummaryApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiDashboardExamples.SUCCESS_ADMIN_DASHBOARD_SUMMARY)
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
    public ResponseEntity<ApiResponse<AdminDashboardSummaryResponse>> getSummary() {
        return ResponseEntity.ok(ApiResponse.success(adminDashboardService.getSummary()));
    }

    @GetMapping("/activity")
    @Operation(
            summary = "대시보드 활동 추이 조회(관리자)",
            description = "Asia/Seoul 기준 최근 7일 또는 30일의 회원 가입/문의/신고/파티 생성 추이를 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiDashboardSchemas.AdminDashboardActivityApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiDashboardExamples.SUCCESS_ADMIN_DASHBOARD_ACTIVITY)
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
                    description = "days 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "invalid_days", value = OpenApiDashboardExamples.ERROR_ADMIN_DASHBOARD_INVALID_DAYS)
                    )
            )
    })
    public ResponseEntity<ApiResponse<AdminDashboardActivityResponse>> getActivity(
            @Parameter(
                    description = "조회 일수(오늘 포함). 7 또는 30만 허용합니다.",
                    schema = @Schema(allowableValues = {"7", "30"}, defaultValue = "7")
            )
            @RequestParam(name = "days", defaultValue = "7") int days
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminDashboardService.getActivity(days)));
    }

    @GetMapping("/recent-items")
    @Operation(
            summary = "대시보드 최근 운영 항목 조회(관리자)",
            description = "문의/신고/앱 공지/파티의 최근 생성 항목을 createdAt DESC 통합 피드로 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiDashboardSchemas.AdminDashboardRecentItemListApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiDashboardExamples.SUCCESS_ADMIN_DASHBOARD_RECENT_ITEMS)
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
                    description = "limit 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "invalid_limit", value = OpenApiDashboardExamples.ERROR_ADMIN_DASHBOARD_INVALID_LIMIT)
                    )
            )
    })
    public ResponseEntity<ApiResponse<List<AdminDashboardRecentItemResponse>>> getRecentItems(
            @Parameter(
                    description = "통합 피드 최대 항목 수",
                    schema = @Schema(defaultValue = "10", minimum = "1", maximum = "30")
            )
            @RequestParam(name = "limit", defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminDashboardService.getRecentItems(limit)));
    }
}
