package com.skuri.skuri_backend.domain.app.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.domain.support.dto.response.AppVersionResponse;
import com.skuri.skuri_backend.domain.support.service.AppVersionService;
import com.skuri.skuri_backend.infra.openapi.OpenApiCommonExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiSupportExamples;
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
@RequestMapping("/v1/app-versions")
@Tag(name = "Support App Version API", description = "앱 버전 공개 조회 API")
public class AppVersionController {

    private final AppVersionService appVersionService;

    @GetMapping("/{platform}")
    @Operation(
            summary = "앱 버전 정보 조회",
            description = "로그인 전에도 호출 가능한 공개 API입니다.",
            security = @SecurityRequirement(name = "")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiSupportExamples.SUCCESS_APP_VERSION)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 platform",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_INVALID_REQUEST)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "앱 버전 정보 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "app_version_not_found", value = OpenApiSupportExamples.ERROR_APP_VERSION_NOT_FOUND)
                    )
            )
    })
    public ResponseEntity<ApiResponse<AppVersionResponse>> getAppVersion(
            @Parameter(description = "플랫폼", example = "ios")
            @PathVariable String platform
    ) {
        return ResponseEntity.ok(ApiResponse.success(appVersionService.getAppVersion(platform)));
    }
}
